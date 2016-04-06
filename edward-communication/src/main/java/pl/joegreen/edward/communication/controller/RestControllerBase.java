package pl.joegreen.edward.communication.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import pl.joegreen.edward.communication.controller.exception.*;
import pl.joegreen.edward.communication.services.LoggedUserNameProvider;
import pl.joegreen.edward.communication.services.VersionProvider;
import pl.joegreen.edward.core.model.IdentifierProvider;
import pl.joegreen.edward.core.model.communication.IdContainer;
import pl.joegreen.edward.management.service.ExecutionManagerService;
import pl.joegreen.edward.management.service.VolunteerManagerService;
import pl.joegreen.edward.persistence.dao.*;

public class RestControllerBase {

    protected final static Logger logger = LoggerFactory
            .getLogger(RestControllerBase.class);

    @Autowired
    protected ProjectDao projectDao;

    @Autowired
    protected JobDao jobDao;

    @Autowired
    protected TaskDao taskDao;

    @Autowired
    protected JsonDataDao jsonDataDao;

    @Autowired
    protected ExecutionDao executionDao;

    @Autowired
    protected ExecutionManagerService executionManagerService;

    @Autowired
    protected VolunteerManagerService volunteerManagerService;

    @Autowired
    protected  UserDao userDao;

    @Autowired
    protected  LoggedUserNameProvider loggedUserNameProvider;


    @Autowired
    protected VersionProvider versionProvider;

    protected <EdwardModel extends IdentifierProvider> EdwardModel getById(
            Long id, EdwardDao<EdwardModel, ?> dao) {
        EdwardModel result = dao.getById(id);
        if (result == null) {
            throw new NotFoundException();
        }
        return result;
    }

    protected <EdwardModel extends IdentifierProvider> IdContainer insertOrUpdate(
            EdwardModel object, EdwardDao<EdwardModel, ?> dao) {
        try {
            dao.save(object);
        } catch (ObjectDoesntExistException e) {
            throw new UpdateNonExistingException(
                    "Cannot update object because it does not exist.", e);
        } catch (InvalidObjectException e) {
            throw new InsertInvalidDataException(
                    "Cannot create or update object because its data is invalid.",
                    e);
        }
        return new IdContainer(object.getId());
    }

    protected <EdwardModel extends IdentifierProvider> void delete(
            Long objectId, EdwardDao<EdwardModel, ?> dao) {
        try {
            dao.delete(objectId);
        } catch (ObjectDoesntExistException e) {
            throw new DeleteNonExistingException(
                    "Cannot delete object because it doesn't exist", e);
        }
    }

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<Object> handleNotFound(NotFoundException exception) {
        logger.debug("Returning " + HttpStatus.NOT_FOUND, exception);
        return new ResponseEntity<Object>(HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Object> handleIntegrityViolation(
            DataIntegrityViolationException exception) {
        logger.debug("Constraint violation - returning "
                + HttpStatus.BAD_REQUEST, exception);
        return new ResponseEntity<Object>(HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(UpdateNonExistingException.class)
    public ResponseEntity<Object> handleBadUpdate(
            UpdateNonExistingException exception) {
        logger.debug("Update cannot be performed - returning "
                + HttpStatus.BAD_REQUEST, exception);
        return new ResponseEntity<Object>(HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(InsertInvalidDataException.class)
    public ResponseEntity<Object> handleInvalidData(
            InsertInvalidDataException exception) {
        logger.debug("Insert or udpate cannot be performed - returning "
                + HttpStatus.BAD_REQUEST, exception);
        return new ResponseEntity<Object>(HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(DeleteNonExistingException.class)
    public ResponseEntity<Object> handleBadDelete(
            DeleteNonExistingException exception) {
        logger.debug("Delete cannot be performed - returning "
                + HttpStatus.BAD_REQUEST, exception);
        return new ResponseEntity<Object>(HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(NoTaskForClientException.class)
    public ResponseEntity<Object> handleNoTask(
            NoTaskForClientException exception) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new ResponseEntity<Object>("{}", headers, HttpStatus.OK);
    }


    @RequestMapping(value = "version", method = RequestMethod.GET, produces = MediaType.TEXT_PLAIN_VALUE)
    @ResponseBody
    public String getVersion() {
        return versionProvider.getVersion();
    }
}
