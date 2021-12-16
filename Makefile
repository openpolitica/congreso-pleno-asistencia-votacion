all: clean build run prepare serve

build:
	./mvnw clean install

clean:
	rm -f *.db*

prepare:
	datasette install datasette-publish-vercel
	sqlite-utils enable-fts *.db votacion_resultado asunto etiquetas
	sqlite-utils enable-fts *.db votacion_grupo_parlamentario asunto etiquetas
	sqlite-utils enable-fts *.db votacion_congresista asunto etiquetas


run:
	./mvnw exec:java -Dexec.mainClass="op.congreso.pleno.LoadPlenos"

serve:
	datasette *.db

csvs-to-sqlite:
	rm -f plenos.db
	csvs-to-sqlite plenos.csv plenos.db
