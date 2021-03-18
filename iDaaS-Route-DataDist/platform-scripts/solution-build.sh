# Change Directory to solution on local machine
echo $PWD
echo "iDAAS - Connect Data Distribution"
cd $PWD
cd ../

mvn clean install
echo "Maven Build Completed"
mvn package
echo "Maven Release Completed"
cd target
cp input-data-*.jar input-data-transform-1.0.jar
echo "Copied Release Specific Version to General version"
