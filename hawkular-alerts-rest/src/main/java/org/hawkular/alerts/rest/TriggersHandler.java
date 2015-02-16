/*
 * Copyright 2015 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hawkular.alerts.rest;

import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;
import org.hawkular.alerts.api.model.condition.Condition;
import org.hawkular.alerts.api.model.trigger.Trigger;
import org.hawkular.alerts.api.services.DefinitionsService;
import org.jboss.logging.Logger;

import javax.ejb.EJB;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;

/**
 * REST endpoint for triggers
 *
 * @author Jay Shaughnessy
 * @author Lucas Ponce
 */
@Path("/triggers")
@Api(value = "/triggers",
     description = "Create/Read/Update/Delete operations for Triggers definitions")
public class TriggersHandler {
    private static final Logger log = Logger.getLogger(TriggersHandler.class);

    @EJB
    DefinitionsService definitions;

    public TriggersHandler() {
        log.debugf("Creating instance.");
    }

    @GET
    @Path("/")
    @Produces(APPLICATION_JSON)
    @ApiOperation(value = "Find all triggers definitions",
                  responseClass = "Collection<org.hawkular.alerts.api.model.trigger.Trigger>",
                  notes = "Pagination is not yet implemented")
    public void findAllTriggers(@Suspended final AsyncResponse response) {
        try {
            Collection<Trigger> triggerList = definitions.getTriggers();
            if (triggerList.isEmpty()) {
                log.debugf("GET - findAllTriggers - Empty");
                response.resume(Response.status(Response.Status.NO_CONTENT).type(APPLICATION_JSON_TYPE).build());
            } else {
                log.debugf("GET - findAllTriggers - %s triggers ", triggerList.size());
                response.resume(Response.status(Response.Status.OK)
                        .entity(triggerList).type(APPLICATION_JSON_TYPE).build());
            }
        } catch (Exception e) {
            log.debugf(e.getMessage(), e);
            Map<String, String> errors = new HashMap<String, String>();
            errors.put("errorMsg", "Internal Error: " + e.getMessage());
            response.resume(Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(errors).type(APPLICATION_JSON_TYPE).build());
        }
    }

    @POST
    @Path("/")
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    @ApiOperation(value = "Create a new trigger definitions",
                  responseClass = "org.hawkular.alerts.api.model.trigger.Trigger",
                  notes = "Returns Trigger created if operation finished correctly")
    public void createTrigger(@Suspended final AsyncResponse response,
                              @ApiParam(value = "Trigger definition to be created",
                                        name = "trigger",
                                        required = true)
                              final Trigger trigger) {
        try {
            if (trigger != null && trigger.getId() != null && definitions.getTrigger(trigger.getId()) == null) {
                log.debugf("POST - createTrigger - triggerId %s ", trigger.getId());
                definitions.addTrigger(trigger);
                response.resume(Response.status(Response.Status.OK)
                        .entity(trigger).type(APPLICATION_JSON_TYPE).build());
            } else {
                log.debugf("POST - createTrigger - ID not valid or existing trigger");
                Map<String, String> errors = new HashMap<String, String>();
                errors.put("errorMsg", "Existing trigger or invalid ID");
                response.resume(Response.status(Response.Status.BAD_REQUEST)
                        .entity(errors).type(APPLICATION_JSON_TYPE).build());
            }
        } catch (Exception e) {
            log.debugf(e.getMessage(), e);
            Map<String, String> errors = new HashMap<String, String>();
            errors.put("errorMsg", "Internal Error: " + e.getMessage());
            response.resume(Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(errors).type(APPLICATION_JSON_TYPE).build());
        }
    }

    @GET
    @Path("/{triggerId}")
    @Produces(APPLICATION_JSON)
    @ApiOperation(value = "Get an existing trigger definition",
                  responseClass = "org.hawkular.alerts.api.model.trigger.Trigger")
    public void getTrigger(@Suspended final AsyncResponse response,
                           @ApiParam(value = "Trigger definition id to be retrieved",
                                     required = true)
                           @PathParam("triggerId") final String triggerId) {
        try {
            Trigger found = null;
            if (triggerId != null && !triggerId.isEmpty()) {
                found = definitions.getTrigger(triggerId);
            }
            if (found != null) {
                log.debugf("GET - getTrigger - triggerId: %s ", found.getId());
                response.resume(Response.status(Response.Status.OK).entity(found).type(APPLICATION_JSON_TYPE).build());
            } else {
                log.debugf("GET - getTrigger - triggerId: %s not found or invalid. ", triggerId);
                Map<String, String> errors = new HashMap<String, String>();
                errors.put("errorMsg", "Trigger ID " + triggerId + " not found or invalid ID");
                response.resume(Response.status(Response.Status.NOT_FOUND)
                        .entity(errors).type(APPLICATION_JSON_TYPE).build());
            }
        } catch (Exception e) {
            log.debugf(e.getMessage(), e);
            Map<String, String> errors = new HashMap<String, String>();
            errors.put("errorMsg", "Internal Error: " + e.getMessage());
            response.resume(Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(errors).type(APPLICATION_JSON_TYPE).build());
        }
    }

    @GET
    @Path("/{triggerId}/conditions")
    @Produces(APPLICATION_JSON)
    @ApiOperation(value = "Get a map with all conditions id an specific trigger.",
                  responseClass = "Collection<Map<String, String>>",
                  notes = "This is a helper for the UI to get all id of the conditions with specific type. " +
                          "It returns a collection of {conditionId: \"value\", className: \"value\" }")
    public void getTriggerConditions(@Suspended final AsyncResponse response,
                                     @ApiParam(value = "Trigger definition id to be retrieved",
                                               required = true)
                                     @PathParam("triggerId") final String triggerId) {
        try {
            Collection<Condition> conditionsList = definitions.getConditions(triggerId);
            Collection<Map<String, String>> conditions = new ArrayList<>();
            for (Condition cond : conditionsList) {
                Map<String, String> conditionsType = new HashMap<String, String>();
                conditionsType.put("conditionId", cond.getConditionId());
                conditionsType.put("className", cond.getClass().getSimpleName());
                conditions.add(conditionsType);
            }
            if (conditions.isEmpty()) {
                log.debugf("GET - getTriggerConditions - Empty");
                response.resume(Response.status(Response.Status.NO_CONTENT).type(APPLICATION_JSON_TYPE).build());
            } else {
                log.debugf("GET - getTriggerConditions - %s conditions ", conditions.size());

                response.resume(Response.status(Response.Status.OK)
                                        .entity(conditions).type(APPLICATION_JSON_TYPE).build());
            }
        } catch (Exception e) {
            log.debugf(e.getMessage(), e);
            Map<String, String> errors = new HashMap<String, String>();
            errors.put("errorMsg", "Internal Error: " + e.getMessage());
            response.resume(Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                                    .entity(errors).type(APPLICATION_JSON_TYPE).build());
        }
    }

    @PUT
    @Path("/{triggerId}")
    @Consumes(APPLICATION_JSON)
    @ApiOperation(value = "Update an existing trigger definition",
                  responseClass = "void")
    public void updateTrigger(@Suspended final AsyncResponse response,
                              @ApiParam(value = "Trigger definition id to be updated",
                                        required = true)
                              @PathParam("triggerId") final String triggerId,
                              @ApiParam(value = "Updated trigger definition",
                                        name = "trigger",
                                        required = true)
                              final Trigger trigger) {
        try {
            if (triggerId != null && !triggerId.isEmpty() &&
                    trigger != null && trigger.getId() != null &&
                    triggerId.equals(trigger.getId()) &&
                    definitions.getTrigger(triggerId) != null) {
                log.debugf("PUT - updateTrigger - triggerId: %s ", triggerId);
                definitions.removeTrigger(triggerId);
                definitions.addTrigger(trigger);
                response.resume(Response.status(Response.Status.OK).build());
            } else {
                log.debugf("PUT - updateTrigger - triggerId: %s not found or invalid. ", triggerId);
                Map<String, String> errors = new HashMap<String, String>();
                errors.put("errorMsg", "Trigger ID " + triggerId + " not found or invalid ID");
                response.resume(Response.status(Response.Status.NOT_FOUND)
                        .entity(errors).type(APPLICATION_JSON_TYPE).build());
            }
        } catch (Exception e) {
            log.debugf(e.getMessage(), e);
            Map<String, String> errors = new HashMap<String, String>();
            errors.put("errorMsg", "Internal Error: " + e.getMessage());
            response.resume(Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(errors).type(APPLICATION_JSON_TYPE).build());
        }
    }

    @DELETE
    @Path("/{triggerId}")
    @ApiOperation(value = "Delete an existing trigger definition",
                  responseClass = "void")
    public void deleteTrigger(@Suspended final AsyncResponse response,
                              @ApiParam(value = "Trigger definition id to be deleted",
                                        required = true)
                              @PathParam("triggerId") final String triggerId) {
        try {
            if (triggerId != null && !triggerId.isEmpty() && definitions.getTrigger(triggerId) != null) {
                log.debugf("DELETE - deleteTrigger - triggerId: %s ", triggerId);
                definitions.removeTrigger(triggerId);
                response.resume(Response.status(Response.Status.OK).build());
            } else {
                log.debugf("DELETE - deleteTrigger - triggerId: %s not found or invalid. ", triggerId);
                Map<String, String> errors = new HashMap<String, String>();
                errors.put("errorMsg", "Trigger ID " + triggerId + " not found or invalid ID");
                response.resume(Response.status(Response.Status.NOT_FOUND)
                        .entity(errors).type(APPLICATION_JSON_TYPE).build());
            }
        } catch (Exception e) {
            log.debugf(e.getMessage(), e);
            Map<String, String> errors = new HashMap<String, String>();
            errors.put("errorMsg", "Internal Error: " + e.getMessage());
            response.resume(Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(errors).type(APPLICATION_JSON_TYPE).build());
        }
    }

}
