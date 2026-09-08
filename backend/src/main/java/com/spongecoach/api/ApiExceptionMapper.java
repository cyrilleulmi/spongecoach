package com.spongecoach.api;

import com.spongecoach.api.dto.ErrorResponse;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class ApiExceptionMapper implements ExceptionMapper<Exception> {

    @Override
    public Response toResponse(Exception exception) {
        if (exception instanceof NotFoundException) {
            return error(Response.Status.NOT_FOUND, "not_found", exception.getMessage());
        }
        if (exception instanceof BadRequestException badRequest) {
            return error(Response.Status.BAD_REQUEST, "bad_request", badRequest.getMessage());
        }
        if (exception instanceof WebApplicationException webApplicationException) {
            Response response = webApplicationException.getResponse();
            return error(
                    Response.Status.fromStatusCode(response.getStatus()),
                    "error",
                    exception.getMessage());
        }
        return error(Response.Status.INTERNAL_SERVER_ERROR, "internal_error", exception.getMessage());
    }

    private Response error(Response.Status status, String code, String message) {
        return Response.status(status)
                .type(MediaType.APPLICATION_JSON)
                .entity(new ErrorResponse(code, message))
                .build();
    }
}
