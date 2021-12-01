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
	./mvnw exec:java -Dexec.mainClass="op.pleno.LoadPlenos"

serve:
	datasette *.db