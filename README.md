# Panel Empresas Vascas

Aaron Duque ya tiene algo entre manos. Una plataforma de inteligencia comercial B2B enfocada al mercado vasco, pensada para descubrir, enriquecer y categorizar empresas de Bizkaia, Gipuzkoa y Araba a partir de datos públicos, scraping web y extracción asistida por LLM.

La idea es sencilla pero ambiciosa: en vez de pagar suscripciones caras a herramientas tipo Apollo o Cognism, construir desde cero un sistema que combine fuentes legítimamente accesibles con inteligencia artificial para extraer datos de contacto y clasificar empresas por tamaño automáticamente. Backend en Java 21 con Spring Boot, base de datos en Supabase y, más adelante, frontend en Next.js para que cualquiera pueda buscar, filtrar y exportar leads cualificados.

## Sobre mí

Desarrollador en Bilbao . Me gusta construir cosas que combinen oficio técnico con utilidad real — desde plataformas SaaS hasta automatizaciones con IA — y este proyecto encaja en esa línea. Más cosas que hago en [aaronduque.es](https://aaronduque.es).

## Objetivos

- [x] Setup inicial Spring Boot + Supabase
- [x] Entidades JPA con soporte para arrays Postgres y JSONB
- [x] Endpoints REST CRUD para empresas
- [x] Importación CSV con mapeo flexible de columnas
- [x] Filtros, paginación y manejo profesional de errores
- [ ] Scraping web con Playwright
- [ ] Extracción de contactos con Gemini
- [ ] Categorización automática por tamaño
- [ ] Frontend Next.js

## Curiosidades dev

al crear el repository que extienda de JpaRepository automaticamente heredas save(), findAll(), findById(), delete(), count(), etc. muy util nos lo ahorramos de escribir

@Repository
public interface EmpresaRepositorio extends JpaRepository<Empresa, Long> 