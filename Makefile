all: clean build carga_plenos prepare registro_plenos prepare_registro_plenos serve

generate: clean build carga_plenos registro_plenos prepare_registro_plenos

test: clean build carga_plenos prepare serve

build:
	./mvnw clean install -T1C

clean:
	rm -f *.db*

prepare:
	sqlite-utils enable-fts *.db votacion_resultado asunto etiquetas
	sqlite-utils enable-fts *.db votacion_grupo_parlamentario asunto etiquetas
	sqlite-utils enable-fts *.db votacion_congresista asunto etiquetas

carga_plenos:
	./mvnw clean compile exec:java -Dexec.mainClass="op.congreso.pleno.app.CargaDetallePlenos" -T1C

extraer_csv:
	./mvnw clean compile exec:java -Dexec.mainClass="op.congreso.pleno.util.ExtraeCsvs" -Dexec.args="${HOME}/Downloads/"

registro_plenos:
	./mvnw exec:java -Dexec.mainClass="op.congreso.pleno.app.CargaRegitroPlenos"

serve:
	datasette *.db

prepare_registro_plenos:
	rm -f plenos.db
	csvs-to-sqlite plenos.csv plenos.db
