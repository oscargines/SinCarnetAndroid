
/****************************************************************************
*						CONSULTAS											*
****************************************************************************/
SELECT DISTINCT
    m.idMunicipio,
    m.Municipio
FROM MUNICIPIOS m
INNER JOIN SEDES s ON s.municipio = m.Municipio
WHERE m.idProvincia = :idProvincia
ORDER BY m.Municipio;

SELECT 
    s.id_juzgado,
    s.nombre,
    s.direccion,
    s.telefono,
    s.codigo_postal
FROM SEDES s
WHERE s.municipio = :municipio
ORDER BY s.nombre;

SELECT 
    p.idProvincia,
    p.Provincia
FROM PROVINCIAS p
WHERE p.idCCAA = 8
ORDER BY p.Provincia;