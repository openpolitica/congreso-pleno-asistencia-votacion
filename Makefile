all: clean build carga_plenos prepare registro_plenos prepare_registro_plenos serve

test: clean build carga_plenos prepare serve

build:
	./mvnw clean install

clean:
	rm -f *.db*

prepare:
	datasette install datasette-publish-vercel
	sqlite-utils enable-fts *.db votacion_resultado asunto etiquetas
	sqlite-utils enable-fts *.db votacion_grupo_parlamentario asunto etiquetas
	sqlite-utils enable-fts *.db votacion_congresista asunto etiquetas


carga_plenos:
	./mvnw exec:java -Dexec.mainClass="op.congreso.pleno.CargaPlenos"

extraer_csv:
	./mvnw exec:java -Dexec.mainClass="op.congreso.pleno.ExtraeCsvs" -Dexec.args="${HOME}/Downloads/"

registro_plenos:
	./mvnw exec:java -Dexec.mainClass="op.congreso.pleno.CargaRegitroPlenos"

serve:
	datasette *.db

prepare_registro_plenos:
	rm -f plenos.db
	csvs-to-sqlite plenos.csv plenos.db
