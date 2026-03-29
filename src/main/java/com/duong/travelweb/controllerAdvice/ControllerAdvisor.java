package com.duong.travelweb.controllerAdvice;

import com.duong.travelweb.model.ErrorResponseDTO;
import com.duong.travelweb.customexception.FieldRequireException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.ArrayList;
import java.util.List;

@ControllerAdvice
public class ControllerAdvisor extends ResponseEntityExceptionHandler {
    @ExceptionHandler(ArithmeticException.class)
    public ResponseEntity<Object> handleArithmeticException(
            ArithmeticException ex, WebRequest request) {
        ErrorResponseDTO errorResponseDTO = new ErrorResponseDTO();
        errorResponseDTO.setError(ex.getMessage());
        List<String> details = new ArrayList<>();
        details.add("Số nguyên không chia được cho 0");
        errorResponseDTO.setDetail(details);
        return new ResponseEntity<>(errorResponseDTO, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(FieldRequireException.class)
    public ResponseEntity<Object> handleFieldRequireException(
            FieldRequireException ex, WebRequest request) {
        ErrorResponseDTO errorResponseDTO = new ErrorResponseDTO();
        errorResponseDTO.setError(ex.getMessage());
        List<String> details = new ArrayList<>();
        details.add("Country is null");
        errorResponseDTO.setDetail(details);
        return new ResponseEntity<>(errorResponseDTO, HttpStatus.BAD_GATEWAY);
    }
}
