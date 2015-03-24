package org.talend.dataprep.api.preparation.service;

import java.io.IOException;
import java.io.InputStream;
import java.lang.Object;
import java.util.Collection;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.talend.dataprep.api.preparation.*;
import org.talend.dataprep.api.preparation.store.ContentCache;
import org.talend.dataprep.metrics.Timed;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;

@RestController
@Api(value = "preparations", basePath = "/preparations", description = "Operations on preparations")
public class PreparationService {

    private static final Log LOGGER = LogFactory.getLog(PreparationService.class);

    @Autowired
    private ContentCache cache;

    @Autowired
    private PreparationRepository versionRepository;

    private final JsonFactory factory = new JsonFactory();

    /**
     * @return Get user name from Spring Security context, return "anonymous" if no user is currently logged in.
     */
    private static String getUserName() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String author;
        if (principal != null) {
            author = principal.toString();
        } else {
            author = "anonymous"; //$NON-NLS-1
        }
        return author;
    }

    @RequestMapping(value = "/preparations", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "List all preparations", notes = "Returns the list of preparations ids the current user is allowed to see. Creation date is always displayed in UTC time zone. See 'preparations/all' to get all details at once.")
    @Timed
    public void list(HttpServletResponse response) {
        response.setHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE); //$NON-NLS-1$
        try (JsonGenerator generator = factory.createGenerator(response.getOutputStream())) {
            generator.writeStartArray();
            for (Preparation preparation : versionRepository.listAll(Preparation.class)) {
                generator.writeString(preparation.id());
            }
            generator.writeEndArray();
            generator.flush();
        } catch (IOException e) {
            throw new RuntimeException("Unexpected I/O exception during message output.", e);
        }
    }

    @RequestMapping(value = "/preparations/all", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "List all preparations", notes = "Returns the list of preparations the current user is allowed to see. Creation date is always displayed in UTC time zone. This operation return all details on the preparations.")
    @Timed
    public Preparation[] listAll(HttpServletResponse response) {
        Collection<Preparation> preparations = versionRepository.listAll(Preparation.class);
        return preparations.toArray(new Preparation[preparations.size()]);
    }

    @RequestMapping(value = "/preparations", method = RequestMethod.PUT, produces = MediaType.TEXT_PLAIN_VALUE)
    @ApiOperation(value = "Create a preparation", notes = "Returns the id of the created preparation.")
    @Timed
    public String create(@ApiParam(value = "content") InputStream preparationContent) {
        try {
            String dataSetId = IOUtils.toString(preparationContent);
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Create new preparation for data set " + dataSetId);
            }
            Preparation preparation = new Preparation(dataSetId, RootStep.INSTANCE);
            preparation.setAuthor(getUserName());
            versionRepository.add(preparation);
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Created new preparation: " + preparation);
            }
            return preparation.id();
        } catch (IOException e) {
            throw new RuntimeException("Unable to create preparation.", e);
        }
    }

    @RequestMapping(value = "/preparations/{id}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Get preparation details", notes = "Return the details of the preparation with provided id.")
    @Timed
    public Preparation get(@ApiParam(value = "id") @PathVariable(value = "id") String id) {
        return versionRepository.get(id, Preparation.class);
    }

    @RequestMapping(value = "/preparations/{id}/content/{version}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Get preparation details", notes = "Return the details of the preparation with provided id.")
    @Timed
    public void get(@ApiParam(value = "id") @PathVariable(value = "id") String id,
            @ApiParam(value = "version") @PathVariable(value = "version") String version, HttpServletResponse response) {
        Preparation preparation = versionRepository.get(id, Preparation.class);
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Get content of preparation #" + id + " at version '" + version + "'.");
        }
        Step step = versionRepository.get(getStepId(version, preparation), Step.class);
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Get content of preparation #" + id + " at step: " + step);
        }
        try {
            if (cache.has(id, step.id())) {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Cache exists for preparation #" + id + " at step " + step);
                }
                ServletOutputStream stream = response.getOutputStream();
                response.setStatus(HttpServletResponse.SC_OK);
                IOUtils.copyLarge(cache.get(id, step.id()), stream);
                stream.flush();
            } else {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Cache does NOT exist for preparation #" + id + " at step " + step);
                }
                response.setStatus(HttpServletResponse.SC_ACCEPTED);
            }
        } catch (IOException e) {
            throw new RuntimeException("Unable to serve content at version #" + version + " for preparation #" + id, e);
        }
    }

    @RequestMapping(value = "/preparations/{id}/actions", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Adds an action to a preparation", notes = "Append an action at end of the preparation with given id.")
    @Timed
    public void append(@ApiParam(value = "id") @PathVariable(value = "id") String id, @ApiParam(value = "action") InputStream body) {
        Preparation preparation = versionRepository.get(id, Preparation.class);
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Adding actions to preparation #" + id);
        }
        if (preparation != null) {
            Step head = preparation.getStep();
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Current head for preparation #" + id + ": " + head);
            }
            // Add a new step
            JSONBlob newContent = ObjectUtils.append(versionRepository.get(head.getContent(), JSONBlob.class), body);
            versionRepository.add(newContent);
            Step newStep = new Step();
            newStep.setContent(newContent.id());
            newStep.setParent(head.id());
            versionRepository.add(newStep);
            preparation.setStep(newStep);
            versionRepository.add(preparation);
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Added head to preparation #" + id + ": head is now " + newStep.id());
            }
        } else {
            LOGGER.error("Preparation #" + id + " does not exist");
            throw new RuntimeException("Preparation id #" + id + " does not exist.");
        }
    }

    @RequestMapping(value = "/preparations/{id}/actions/{version}", method = RequestMethod.GET, produces = MediaType.TEXT_PLAIN_VALUE)
    @ApiOperation(value = "Get the action on preparation at given version.", notes = "Returns the action JSON at version.")
    @Timed
    public String getVersionedAction(@ApiParam(value = "id") @PathVariable(value = "id") String id,
            @ApiParam(value = "version") @PathVariable(value = "version") String version) {
        Preparation preparation = versionRepository.get(id, Preparation.class);
        if (preparation != null) {
            String stepId = getStepId(version, preparation);
            Step step = versionRepository.get(stepId, Step.class);
            return versionRepository.get(step.getContent(), JSONBlob.class).getContent();
        } else {
            throw new RuntimeException("Preparation id #" + id + " does not exist.");
        }
    }

    private static String getStepId(@ApiParam(value = "version") @PathVariable(value = "version") String version,
            Preparation preparation) {
        String stepId;
        if ("head".equalsIgnoreCase(version)) { //$NON-NLS-1$
            stepId = preparation.getStep().id();
        } else if ("origin".equalsIgnoreCase(version)) {
            stepId = RootStep.INSTANCE.id();
        } else {
            stepId = version;
        }
        return stepId;
    }
}
