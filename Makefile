SIMTEL_FILE = ../hb9/gamma_20deg_180deg_run1000___cta-prod3_desert-2150m-Paranal-merged.simtel.gz

instrument:
	python array_definitions.py $(SIMTEL_FILE) ../src/main/resources/array_definitions/cta_array_definition.json
	python camera_definitions.py $(SIMTEL_FILE) ../src/main/resources/camera_definitions/cta_camera_definition.json
	python convert_raw_data.py $(SIMTEL_FILE) ../src/main/resources/data/images.json.gz --limit 10


GAMMA_SIMTELS=../hb9/diffuse/
PROTON_SIMTELS=../hb9/proton/

GAMMA_IMAGES=./build/gamma_images.json.gz
PROTON_IMAGES=./build/proton_images.json.gz

GAMMA_DL2=./build/gamma_dl2.csv
PROTON_DL2=./build/proton_dl2.csv

CLF=./build/clf.json
RGR=./build/rgr.json

JAR=./build/libs/jayct-0.2.0-SNAPSHOT-all.jar

all: $(CLF) $(RGR)


$(build):
	mkdir -p build/libs

$(GAMMA_IMAGES): $(GAMMA_SIMTELS) scripts/convert_raw_data.py | $(build)
	python scripts/convert_raw_data.py $(GAMMA_SIMTELS)/*.simtel.gz  $(GAMMA_IMAGES)

$(PROTON_IMAGES): $(PROTON_SIMTELS) scripts/convert_raw_data.py | $(build)
	python scripts/convert_raw_data.py $(PROTON_SIMTELS)/*.simtel.gz  $(PROTON_IMAGES)

$(JAR): | $(build)
	gradle shadowJar

$(PROTON_DL2): $(JAR) $(PROTON_IMAGES)
	java -cp build/libs/jayct-0.2.0-SNAPSHOT-all.jar DL2Producer $(PROTON_IMAGES)  $(PROTON_DL2)

$(GAMMA_DL2): $(JAR) $(GAMMA_IMAGES)
	java -cp build/libs/jayct-0.2.0-SNAPSHOT-all.jar DL2Producer $(GAMMA_IMAGES)  $(GAMMA_DL2)

$(CLF) $(RGR): $(GAMMA_DL2) $(PROTON_DL2) scripts/train_models.py
	python scripts/train_models.py $(GAMMA_DL2) $(PROTON_DL2) $(CLF) $(RGR)