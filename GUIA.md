# Guía para cargar asistencias y votaciones de plenos del Congreso

## Pasos

1. Verificar que existe un Pleno registrado en [Issues](https://github.com/openpolitica/congreso-pleno-asistencia-votacion/issues?q=is%3Aopen+is%3Aissue+label%3Apleno+)
2. Si el Pleno no existe, [crearlo](https://github.com/openpolitica/congreso-pleno-asistencia-votacion/issues/new?assignees=&labels=pleno&template=pleno.md&title=%5BPleno%5D+2021-08-19) — registrando el numero de paginas del documento PDF — y agregarlo al proyecto de plenos por periodo
3. Asignarte la pagina a cargar, agregando `@nombre_usuario` al lado de la página. Por ejemplo: `- [ ] 1 @jeqo`
4. Una vez registrados la Asistencia/Votacion, crear un issue utilizando la plantilla de [Asistencia](https://github.com/openpolitica/congreso-pleno-asistencia-votacion/issues/new?assignees=&labels=asistencia&template=asistencia.md&title=%5BAsistencia%5D+Pleno+de+Fecha%3A+2021-08-10+Hora%3A+09%3A10) o [Votacion](https://github.com/openpolitica/congreso-pleno-asistencia-votacion/issues/new?assignees=&labels=votacion&template=votacion.md&title=%5BVotaci%C3%B3n%5D+Pleno+de+Fecha%3A+2021-08-10+Hora%3A+09%3A10), según sea el caso; y agregarlo al proyecto respectivo (proyectos por legislatura)
6. Agregar el numero del issue de la Asistencia/Votacion en el issue del Pleno para darle seguimiento

En este punto, el equipo de carga creará un Pull Request para cargar los CSV a partir del archivo de Google Sheet creado y se realizará la validación de la calidad de los datos.
