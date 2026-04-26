package eus.aaronduque.panelempresas.extraccion.service;

import eus.aaronduque.panelempresas.extraccion.dto.DatosExtraidos;

/**
 * Contrato para extractores de datos de contacto basados en LLM.
 */
public interface ExtractorLLM {

    /**
     * Dado un texto plano (típicamente extraído de webs corporativas),
     * extrae datos estructurados de contacto.
     * 
     * @param textoEmpresa texto plano con información de la empresa
     * @return datos extraídos, nunca null (campos vacíos si no encuentra nada)
     */
    DatosExtraidos extraer(String textoEmpresa);
}