package eus.aaronduque.panelempresas.extraccion.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.genai.Client;
import com.google.genai.types.GenerateContentResponse;
import eus.aaronduque.panelempresas.extraccion.dto.DatosExtraidos;
import eus.aaronduque.panelempresas.extraccion.dto.DatosExtraidos.PersonaContacto;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class ExtractorGemini implements ExtractorLLM {

    @Value("${gemini.api-key}")
    private String apiKey;

    @Value("${gemini.modelo:gemini-2.5-flash}")
    private String modelo;

    @Value("${gemini.max-caracteres-entrada:30000}")
    private int maxCaracteresEntrada;

    private Client cliente;
    private final ObjectMapper jsonMapper = new ObjectMapper();

    @PostConstruct
    public void init() {
        this.cliente = Client.builder().apiKey(apiKey).build();
        log.info("ExtractorGemini inicializado con modelo {}", modelo);
    }

    @Override
    public DatosExtraidos extraer(String textoEmpresa) {
        if (textoEmpresa == null || textoEmpresa.isBlank()) {
            return DatosExtraidos.builder().build();
        }

        String textoTruncado = truncar(textoEmpresa);
        String prompt = construirPrompt(textoTruncado);

        try {
            log.debug("Llamando a Gemini con {} caracteres de entrada", textoTruncado.length());
            GenerateContentResponse respuesta = cliente.models.generateContent(
                    modelo, prompt, null);

            String textoRespuesta = respuesta.text();
            log.debug("Respuesta de Gemini: {} caracteres",
                    textoRespuesta != null ? textoRespuesta.length() : 0);
            log.debug("Respuesta cruda de Gemini:\n{}", textoRespuesta);

            return parsearRespuesta(textoRespuesta);

        } catch (Exception e) {
            log.error("Error llamando a Gemini: {}", e.getMessage(), e);
            return DatosExtraidos.builder().build();
        }
    }

    /**
     * Trunca el texto si excede el límite, intentando cortar en un salto de línea.
     */
    private String truncar(String texto) {
        if (texto.length() <= maxCaracteresEntrada)
            return texto;

        String truncado = texto.substring(0, maxCaracteresEntrada);
        int ultimoSalto = truncado.lastIndexOf("\n");
        if (ultimoSalto > maxCaracteresEntrada / 2) {
            truncado = truncado.substring(0, ultimoSalto);
        }
        log.debug("Texto truncado de {} a {} caracteres", texto.length(), truncado.length());
        return truncado;
    }

    /**
     * Construye el prompt con instrucciones precisas para extraer JSON
     * estructurado.
     */
    private String construirPrompt(String texto) {
        return """
                Eres un extractor de datos de contacto de webs corporativas españolas.
                Analiza el siguiente contenido y devuelve EXCLUSIVAMENTE un JSON válido con esta estructura,
                sin markdown, sin explicaciones, sin backticks:

                {
                  "emails_genericos": [],
                  "telefonos": [],
                  "personas": [
                    { "nombre": "string", "cargo": "string", "email": "string|null", "telefono": "string|null" }
                  ],
                  "redes_sociales": { "linkedin": "string|null", "twitter": "string|null", "facebook": "string|null" },
                  "descripcion_breve": "string"
                }

                REGLAS ESTRICTAS:
                - emails_genericos: SOLO emails tipo info@, contacto@, ventas@, comunicacion@... NO incluyas emails de personas físicas aquí.
                - personas: SOLO incluir personas con nombre Y cargo explícitos. NO inventes ni infieras. Si no hay personas claras, devuelve lista vacía.
                - telefonos: formato español con prefijo +34 si es posible. Solo números reales que aparezcan en el texto, no plantillas tipo "+34 XXX XXX".
                - redes_sociales: solo URLs completas que aparezcan en el texto. Si no aparecen, null.
                - descripcion_breve: máximo 200 caracteres, qué hace la empresa según el texto.
                - Si un campo no aparece, devuelve [] o null. NO ALUCINES DATOS.

                Contenido a analizar:
                %s
                """
                .formatted(texto);
    }

    /**
     * Parsea la respuesta de Gemini a DatosExtraidos.
     */
    private DatosExtraidos parsearRespuesta(String texto) {
        if (texto == null || texto.isBlank()) {
            return DatosExtraidos.builder().build();
        }

        // Limpiar posibles bloques de markdown
        String json = texto.trim()
                .replaceAll("(?s)^```json\\s*", "")
                .replaceAll("(?s)^```\\s*", "")
                .replaceAll("(?s)\\s*```$", "")
                .trim();

        try {
            JsonNode raiz = jsonMapper.readTree(json);

            return DatosExtraidos.builder()
                    .emailsGenericos(extraerListaStrings(raiz.path("emails_genericos")))
                    .telefonos(extraerListaStrings(raiz.path("telefonos")))
                    .personas(extraerPersonas(raiz.path("personas")))
                    .redesSociales(extraerRedesSociales(raiz.path("redes_sociales")))
                    .descripcionBreve(textoOpNull(raiz.path("descripcion_breve")))
                    .build();

        } catch (JsonProcessingException e) {
            log.warn("No se pudo parsear la respuesta de Gemini como JSON. Respuesta: {}", json);
            return DatosExtraidos.builder().build();
        }
    }

    private List<String> extraerListaStrings(JsonNode nodo) {
        List<String> resultado = new ArrayList<>();
        if (nodo.isArray()) {
            nodo.forEach(item -> {
                if (item.isTextual() && !item.asText().isBlank()) {
                    resultado.add(item.asText().trim());
                }
            });
        }
        return resultado;
    }

    private List<PersonaContacto> extraerPersonas(JsonNode nodo) {
        List<PersonaContacto> resultado = new ArrayList<>();
        if (!nodo.isArray())
            return resultado;

        nodo.forEach(persona -> {
            String nombre = textoOpNull(persona.path("nombre"));
            String cargo = textoOpNull(persona.path("cargo"));
            if (nombre != null && cargo != null) {
                resultado.add(PersonaContacto.builder()
                        .nombre(nombre)
                        .cargo(cargo)
                        .email(textoOpNull(persona.path("email")))
                        .telefono(textoOpNull(persona.path("telefono")))
                        .build());
            }
        });
        return resultado;
    }

    private Map<String, String> extraerRedesSociales(JsonNode nodo) {
        Map<String, String> resultado = new HashMap<>();
        if (!nodo.isObject())
            return resultado;

        Iterator<Map.Entry<String, JsonNode>> campos = nodo.fields();
        while (campos.hasNext()) {
            Map.Entry<String, JsonNode> campo = campos.next();
            String valor = textoOpNull(campo.getValue());
            if (valor != null) {
                resultado.put(campo.getKey(), valor);
            }
        }
        return resultado;
    }

    private String textoOpNull(JsonNode nodo) {
        if (nodo.isNull() || nodo.isMissingNode())
            return null;
        if (!nodo.isTextual())
            return null;
        String valor = nodo.asText().trim();
        return valor.isBlank() ? null : valor;
    }
}