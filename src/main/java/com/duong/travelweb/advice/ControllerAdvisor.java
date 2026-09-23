package com.duong.travelweb.advice;

import com.duong.travelweb.exception.ApiException;
import com.duong.travelweb.model.dto.ErrorResponseDTO;
import com.duong.travelweb.exception.FieldRequireException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
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
        return new ResponseEntity<>(errorResponseDTO, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<Object> handleApiException(ApiException ex, WebRequest request) {
        ErrorResponseDTO errorResponseDTO = new ErrorResponseDTO();
        errorResponseDTO.setError(ex.getMessage());
        return new ResponseEntity<>(errorResponseDTO, ex.getStatus());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Object> handleDataIntegrityViolation(DataIntegrityViolationException ex, WebRequest request) {
        ErrorResponseDTO errorResponseDTO = new ErrorResponseDTO();
        errorResponseDTO.setError("Dữ liệu bị trùng hoặc vi phạm ràng buộc");
        return new ResponseEntity<>(errorResponseDTO, HttpStatus.CONFLICT);
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
                                                                  HttpHeaders headers,
                                                                  HttpStatusCode status,
                                                                  WebRequest request) {
        ErrorResponseDTO errorResponseDTO = new ErrorResponseDTO();
        List<String> details = new ArrayList<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(error -> details.add(error.getField() + ": " + error.getDefaultMessage()));
        errorResponseDTO.setError(details.isEmpty() ? "Dữ liệu không hợp lệ" : details.get(0).split(": ", 2)[1]);
        errorResponseDTO.setDetail(details);
        return new ResponseEntity<>(errorResponseDTO, HttpStatus.BAD_REQUEST);
    }
}
