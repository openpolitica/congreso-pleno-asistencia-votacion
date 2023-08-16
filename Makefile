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

run:
	./mvnw clean compile

serve:
	datasette *.db

prepare_registro_plenos:
	rm -f plenos.db
	csvs-to-sqlite data/2021-2026/plenos.csv plenos.db

new_pleno:
	BRANCH=$(cat pr-branch.txt) && git checkout -b ${BRANCH}
	git add data
	COMMIT=$(cat pr-title.txt) && git commit -m ${COMMIT}
	BRANCH=$(cat pr-branch.txt) && git push origin ${BRANCH}