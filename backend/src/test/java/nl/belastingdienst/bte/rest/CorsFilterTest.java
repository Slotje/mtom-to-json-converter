package nl.belastingdienst.bte.rest;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CorsFilterTest {

    @Test
    void testFilterAddsCorsHeaders() {
        CorsFilter filter = new CorsFilter();

        ContainerRequestContext requestContext = mock(ContainerRequestContext.class);
        ContainerResponseContext responseContext = mock(ContainerResponseContext.class);
        MultivaluedMap<String, Object> headers = new MultivaluedHashMap<>();
        when(responseContext.getHeaders()).thenReturn(headers);

        filter.filter(requestContext, responseContext);

        assertTrue(headers.containsKey("Access-Control-Allow-Origin"));
        assertEquals("*", headers.getFirst("Access-Control-Allow-Origin"));

        assertTrue(headers.containsKey("Access-Control-Allow-Headers"));
        assertEquals("Content-Type, Authorization, Accept", headers.getFirst("Access-Control-Allow-Headers"));

        assertTrue(headers.containsKey("Access-Control-Allow-Methods"));
        assertEquals("GET, POST, PUT, DELETE, PATCH, OPTIONS", headers.getFirst("Access-Control-Allow-Methods"));

        assertTrue(headers.containsKey("Access-Control-Expose-Headers"));
        assertEquals("Content-Disposition", headers.getFirst("Access-Control-Expose-Headers"));
    }
}
