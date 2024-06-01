package io.github.stefankoppier.mapping.resolving.classes

import io.github.stefankoppier.mapping.MappingPluginContext
import io.github.stefankoppier.mapping.resolving.*
import io.github.stefankoppier.mapping.util.error
import io.github.stefankoppier.mapping.util.location
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.types.getClass
import org.jetbrains.kotlin.ir.util.getPropertyGetter
import org.jetbrains.kotlin.ir.util.properties

class ClassMappingResolver(pluginContext: MappingPluginContext)
    : BaseVisitor<ConstructorCallMapping, Unit>(pluginContext) {

    override fun visitFunction(declaration: IrFunction, data: Unit): ConstructorCallMapping {
        val sourceParameter = requireNotNull(declaration.valueParameters.firstOrNull())
        val mappingTarget = declaration.accept(TargetsCollector(pluginContext), Unit) as ConstructorMappingTarget
        val sourceClass = requireNotNull(sourceParameter.type.getClass()) {
            "Expected type of source argument to be non-null."
        }

        val dispatchReceiverSymbol = declaration.valueParameters.first().symbol
        val concreteSources = declaration.body?.accept(ObjectSourcesCollector(pluginContext, dispatchReceiverSymbol), Unit) ?: emptyList()

        val sources: MutableList<MappingSource> = mappingTarget.values.map { target ->
            val concreteSource = concreteSources.firstOrNull { it.first == target.name }

            if (concreteSource != null) {
                concreteSource.second
            }
            else {
                val source = requireNotNull(sourceClass.properties.firstOrNull { it.name == target.name }) {
                    pluginContext.messageCollector.error(
                        "Target ${target.name.asString()} has no source defined",
                        location(declaration)
                    )
                }
                PropertySource(
                    sourceClass.getPropertyGetter(source.name.asString())!!,
                    target.type,
                    sourceParameter.symbol,
                )
            }
        }.toMutableList()

        return ConstructorCallMapping(sources)
    }
}