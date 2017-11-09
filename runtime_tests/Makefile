jar = ../build/libs/jayct-0.1.0-SNAPSHOT-all.jar

all: ./build/plots/result.pdf

./build/FINISHED_MEASUREMENT: $(jar)
	mkdir -p ./build/
	rm -f ./build/FINISHED_MEASUREMENT
	for i in {1..24}; do \
		echo "Using $$i threads" ; \
		java -jar $(jar) -l 360 -p 1 -s 1 -m $$i -w $$i ../src/main/resources/images.json.gz ../src/main/resources/classifier.json ./build/performance_$$i.csv; \
 	done
	touch ./build/FINISHED_MEASUREMENT


./build/plots/result.pdf: ./build/FINISHED_MEASUREMENT
	mkdir -p ./build/plots/
	python3 plot_csv_results.py ./build/ -o ./build/plots/result.pdf

$(jar): ../build.gradle $(shell find ../src/ -type f)
	echo "building package"
	cd ../; ./gradlew shadowJar --parallel; cd -
