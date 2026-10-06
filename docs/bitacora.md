# Bitácora Práctica 10

## Experimento A3: Coil con su propio cliente
Cree la rama experimento-a3 y quité la línea de components de ImagenesModule.kt para que Coil usara su cliente por omisión. Al abrir la app, las imágenes que ya había abierto antes seguían cargando porque estaban guardadas en el caché de disco del teléfono.
Borré el caché con adb shell run-as mx.tec.avisos rm -rf cache/image_cache y creé un aviso nuevo. En la app la tarjeta se quedó en un recuadro gris y en Logcat no apareció ninguna petición de imagen porque ese cliente no tiene nuestro interceptor. Al ver los logs del servidor en Docker con docker compose logs api, confirmé que la petición sí llegaba pero el servidor la rechazaba con 401 Unauthorized por no llevar el token. Esto comprobó que Coil debe compartir el mismo OkHttpClient de la app para poder incluir la autorización.

## Experimento D2: Subir la imagen sin reducir
Cree la rama experimento-d2 y cambié las constantes en CompresorDeImagen.kt a LADO_MAX = 8000 y CALIDAD = 100. Al intentar publicar una foto de galería de 4032x3024 que pesaba 2095 KB, el compresor generó un archivo de 3676 KB. El archivo aumentó de tamaño en lugar de reducirse porque comprimir en calidad 100 codifica con detalle todo el ruido del JPEG original.
Al enviar la petición, el servidor respondió con un error 413 Request Entity Too Large porque el archivo superó el límite de 2 MB. Además, la pantalla de la app solo mostraba "El servidor respondió 413." sin dar detalles, lo que demostró la necesidad de no saltarse la compresión y de adaptar ApiErrors.kt para mostrar el mensaje real del backend.

## Ejercicio D4: Decisiones para el reto
Para la imagen principal de nuestro reto (la foto de evidencia de un reporte):
- Tamaño y LADO_MAX: En la pantalla más grande se verá a lo ancho de un teléfono, que son aproximadamente 1080 px físicos. Con ese número elegimos LADO_MAX = 1280 px con calidad 80. Deja suficiente margen de nitidez para ver detalles sin gastar datos ni memoria de más, dejando el archivo en menos de 150 KB.
- Quién puede ver la imagen: Cualquiera con sesión iniciada. En GET /api/imagenes/{clave} se exige el token de autenticación igual que en esta práctica. Si fuera una imagen privada (como una identificación o perfil), en el backend se guardaría el usuario creador y en el GET se verificaría que coincida con el token antes de responder.
- Imágenes huérfanas: Si el usuario sube la imagen pero se arrepiente y no publica el reporte, la imagen se queda guardada en el almacén de objetos sin que nadie la use. En producción la solución es poner una regla de ciclo de vida (TTL) en el almacén de objetos para que borre automáticamente cualquier archivo que lleve más de 24 horas sin asociarse a un registro de la base de datos.
