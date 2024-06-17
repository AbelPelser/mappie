package io.github.mappie.generation

import io.github.mappie.resolving.ConstructorCallMapping
import io.github.mappie.resolving.EnumMapping
import io.github.mappie.resolving.Mapping
import io.github.mappie.resolving.SingleValueMapping
import io.github.mappie.resolving.classes.PropertySource
import io.github.mappie.validation.MappingValidation

interface MappingSelector {

    fun select(): Pair<Mapping, MappingValidation>

    private class ConstructorMappingSelector(private val mappings: List<Pair<ConstructorCallMapping, MappingValidation>>) : MappingSelector {

        override fun select(): Pair<Mapping, MappingValidation> {
            return selectPrimary() ?: selectLeastResolvedAutomatically()
        }

        private fun selectPrimary(): Pair<Mapping, MappingValidation>? =
            mappings.firstOrNull { it.first.symbol.owner.isPrimary }

        private fun selectLeastResolvedAutomatically(): Pair<Mapping, MappingValidation> =
            mappings.maxBy { it.first.mappings.count { (_, sources) -> !(sources.single() as PropertySource).isResolvedAutomatically } }
    }

    companion object {
        fun of(mappings: List<Pair<Mapping, MappingValidation>>): MappingSelector =
            when {
                mappings.all { it.first is ConstructorCallMapping } -> ConstructorMappingSelector(mappings.map { it.first as ConstructorCallMapping to it.second })
                mappings.all { it.first is EnumMapping } -> object : MappingSelector { override fun select() = mappings.single() }
                mappings.all { it.first is SingleValueMapping } -> object : MappingSelector { override fun select() = mappings.single() }
                else -> error("Not all mappings are of the same type. This is a bug.")
            }
    }
}