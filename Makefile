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
	./mvnw clean compile exec:java -Dexec.mainClass="op.congreso.pleno.app.LoadDetallePlenosAndSave" -T1C

extraer_csv:
	./mvnw clean compile exec:java -Dexec.mainClass="op.congreso.pleno.util.ExtraeCsvs" -Dexec.args="${HOME}/Downloads/"

registro_plenos:
	./mvnw exec:java -Dexec.mainClass="op.congreso.pleno.app.LoadRegitroPlenoDocuments"

serve:
	datasette *.db

prepare_registro_plenos:
	rm -f plenos.db
	csvs-to-sqlite plenos.csv plenos.db

new_pleno:
	git add plenos.csv
	git commit -m 'update plenos'
	git push origin main
	BRANCH=$(cat pr-branch.txt) && git checkout -b ${BRANCH}
	git add data
	COMMIT=$(cat pr-title.txt) && git commit -m ${COMMIT}
	BRANCH=$(cat pr-branch.txt) && git push origin ${BRANCH}