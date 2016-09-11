/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.spring.dsl.model;

import static org.mule.runtime.core.config.i18n.MessageFactory.createStaticMessage;
import org.mule.runtime.config.spring.dsl.api.ComponentBuildingDefinition;
import org.mule.runtime.config.spring.dsl.processor.AbstractAttributeDefinitionVisitor;
import org.mule.runtime.core.api.MuleRuntimeException;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class MinimalApplicationModelGenerator {

  private final ComponentBuildingDefinitionRegistry componentBuildingDefinitionRegistry;
  private ApplicationModel applicationModel;

  public MinimalApplicationModelGenerator(ApplicationModel applicationModel,
                                          ComponentBuildingDefinitionRegistry componentBuildingDefinitionRegistry) {
    this.applicationModel = applicationModel;
    this.componentBuildingDefinitionRegistry = componentBuildingDefinitionRegistry;
  }

  public ApplicationModel getMinimalModelForProcessor(String processorPath)
  {
    String[] parts = processorPath.split("/");
    String flowName = parts[0];
    ComponentModel flowModel = findRequiredComponentModel(flowName);
    //TODO variable not used becouse the object itselft is being chnaged
    ComponentModel minimumFlowModel = filterFlowModelParts(flowModel, parts);
    return getMinimalModelForComponent(flowModel.getNameAttribute());
  }

  public ApplicationModel getMinimalModelForComponent(String name) {
    ComponentModel requestedComponentModel = findRequiredComponentModel(name);
    final Set<String> otherRequiredGlobalComponents = resolveComponentDependencies(requestedComponentModel);
    otherRequiredGlobalComponents.add(name);
    Set<String> allRequiredComponentModels = findComponentModelsDependencies(otherRequiredGlobalComponents);
    //List<String> allNamedElements = applicationModel.getRootComponentModel().getInnerComponents().stream()
    //        .filter(componentModel -> null != componentModel.getNameAttribute())
    //    .map(componentModel -> componentModel.getNameAttribute()).collect(Collectors.toList());
    Iterator<ComponentModel> iterator = applicationModel.getRootComponentModel().getInnerComponents().iterator();
    while (iterator.hasNext()) {
      ComponentModel componentModel = iterator.next();
      if (componentModel.getNameAttribute() == null || !allRequiredComponentModels.contains(componentModel.getNameAttribute())) {
        iterator.remove();
      }
    }
    return applicationModel;
  }

  private ComponentModel filterFlowModelParts(ComponentModel flowModel, String[] parts)
  {
    ComponentModel currentLevelModel = flowModel;
    for (int i = 1; i < parts.length; i++)
    {
      int selectedPath = Integer.parseInt(parts[i]);
      List<ComponentModel> innerComponents = currentLevelModel.getInnerComponents();
      Iterator<ComponentModel> iterator = innerComponents.iterator();
      int currentElement = 0;
      while (iterator.hasNext())
      {
        if (currentElement != selectedPath)
        {
          iterator.remove();
        }
        else
        {
          currentLevelModel = iterator.next();
        }
      }
    }
    return flowModel;
  }

  private ComponentModel findRequiredComponentModel(String name) {
    return applicationModel.findNamedComponent(name)
        .orElseThrow(() -> new MuleRuntimeException(createStaticMessage("No named component with name " + name)));
  }

  private Set<String> findComponentModelsDependencies(Set<String> componentModelsNames) {
    Set<String> componentsToSearchDependencies = componentModelsNames;
    Set<String> foundDependencies = new HashSet<>();
    Set<String> alreadySearchedDependencies = new HashSet<>();
    do {
      componentsToSearchDependencies.addAll(foundDependencies);
      for (String componentModelName : componentsToSearchDependencies) {
        if (!alreadySearchedDependencies.contains(componentModelName)) {
          alreadySearchedDependencies.add(componentModelName);
          foundDependencies.addAll(resolveComponentDependencies(findRequiredComponentModel(componentModelName)));
        }
      }
      foundDependencies.addAll(componentModelsNames);

    } while (!foundDependencies.containsAll(componentsToSearchDependencies));
    return foundDependencies;
  }


  /**
   * Evaluates the component model and all its children to see what are the
   * dependencies this component model has over other configuration component model.
   *
   * This method does not take into account the dependencies of the found dependencies.
   *
   * @param requestedComponentModel
   * @return
   */
  private Set<String> resolveComponentDependencies(ComponentModel requestedComponentModel) {
    Set<String> otherDependencies = new HashSet<>();
    requestedComponentModel.getInnerComponents()
        .stream().forEach(childComponent -> {
      otherDependencies.addAll(resolveComponentDependencies(childComponent));
        });
    final Set<String> parametersReferencingDependencies = new HashSet<>();
    //TODO MULE-10516 - Remove one the config-ref attribute is defined as a reference
    parametersReferencingDependencies.add("config-ref");
    ComponentBuildingDefinition buildingDefinition =
        componentBuildingDefinitionRegistry.getBuildingDefinition(requestedComponentModel.getIdentifier())
            .orElseThrow(() -> new MuleRuntimeException(createStaticMessage("No component building definition for component "
                + requestedComponentModel.getIdentifier())));

    buildingDefinition.getAttributesDefinitions()
        .stream().forEach(attributeDefinition -> {
          attributeDefinition.accept(new AbstractAttributeDefinitionVisitor() {
            @Override
            public void onReferenceSimpleParameter(String reference) {
              parametersReferencingDependencies.add(reference);
            }
          });
        });

    for (String parametersReferencingDependency : parametersReferencingDependencies)
    {
      if (requestedComponentModel.getParameters().containsKey(parametersReferencingDependency))
      {
        otherDependencies.add(requestedComponentModel.getParameters().get(parametersReferencingDependency));
      }
    }
    return otherDependencies;
  }


}
