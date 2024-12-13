package org.matsim.activitySimtoMATSim;

import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.opengis.feature.simple.SimpleFeature;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class planTransfer {

    public static void main(String[] args) {
        Config config = ConfigUtils.createConfig();
        config.global().setCoordinateSystem("EPSG:28992");
        Scenario scenario = ScenarioUtils.loadScenario(config);
        Population population = scenario.getPopulation();
        PopulationFactory populationFactory = population.getFactory();

        String agentAttribute = "scenarios/MRDH/activitySimDemand/sample_persons10Perc.csv";
        String agentTrip = "scenarios/MRDH/activitySimDemand/agentTrip10Perc.csv";
        String nlShape = "scenarios/NLShape/areas_landuse_2016.shp";
//        for agents cannot find respective home zone in the shapefile
        String missingGeometryLog = "scenarios/MRDH/missing_geometry_log.csv";


//        read the shapefile
        Collection<SimpleFeature> features = ShapeFileReader.getAllFeatures(nlShape);

        Map<Integer, Geometry> zoneGeometries  = new HashMap<>();

        for (SimpleFeature feature : features) {
            int subzone = Integer.parseInt(feature.getAttribute("SUBZONE0").toString()) + 1;
            Geometry geometry = (Geometry) feature.getDefaultGeometry();
            zoneGeometries.put(subzone, geometry);
        }

        //        Create hashmap to store <personID, activity arrayList>
        HashMap<String, ArrayList<String>> idAndAllActivities = new HashMap<>();
        try {
            BufferedReader activityReader = new BufferedReader(new FileReader(agentTrip));
            String agentActivity = null;

            while ((agentActivity = activityReader.readLine()) != null){
                String[] activitySpilted = agentActivity.split(",");
                String activityAgentID = activitySpilted[1];

                if (idAndAllActivities.containsKey(activityAgentID)){
                    ArrayList<String> existentActivities = idAndAllActivities.get(activityAgentID);
                    existentActivities.add(agentActivity);
                } else {
                    ArrayList<String> activities = new ArrayList<String>();
                    activities.add(agentActivity);
                    idAndAllActivities.put(activityAgentID, activities);
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        System.out.println("finish reading Trips!");

        try (BufferedReader attributeReader = new BufferedReader(new FileReader(agentAttribute));
             BufferedWriter missingGeometryWriter = new BufferedWriter(new FileWriter(missingGeometryLog))) {

            missingGeometryWriter.write("PersonID,HomeZone\n");
            String agent = null;

            while ((agent = attributeReader.readLine()) != null){
                String[] attributeSpilted = agent.split(",");
                String attributeAgentID = attributeSpilted[0];
                String agentHousehold = attributeSpilted[1];
                String agentAge = attributeSpilted[2];
                String agentPNUM = attributeSpilted[3];
                String agentGender = attributeSpilted[4];
                String agentPEmploy = attributeSpilted[5];
                String agentPStudent = attributeSpilted[6];
                String agentPType = attributeSpilted[7];
                String agentEducation = attributeSpilted[9];
                String agentMaaSSubsciption = attributeSpilted[14];

                int homeZone = Integer.parseInt(attributeSpilted[38]);

                Person person = populationFactory.createPerson(Id.createPersonId(attributeAgentID));
                person.getAttributes().putAttribute("household", agentHousehold);
                person.getAttributes().putAttribute("age", agentAge);
                person.getAttributes().putAttribute("PNUM", agentPNUM);
                person.getAttributes().putAttribute("gender", agentGender);
                person.getAttributes().putAttribute("PEmploy", agentPEmploy);
                person.getAttributes().putAttribute("PStudent", agentPStudent);
                person.getAttributes().putAttribute("Education", agentEducation);
                person.getAttributes().putAttribute("MaaSSubscription", agentMaaSSubsciption);
                person.getAttributes().putAttribute("PType", agentPType);

                ArrayList<String> agentFullTrip = idAndAllActivities.get(attributeAgentID);

                Plan plan = populationFactory.createPlan();

                if (agentFullTrip != null){
//                    agents with Trip throughout the simulated day

                    for (int i = 0; i < agentFullTrip.size(); i++){

                        if (i == 0) {
//                            create fist Home activity

                            String tripsInArray = agentFullTrip.get(i);
                            String[] trip = tripsInArray.split(",");

                            String activityPurpose = "Home".concat("_").concat(String.valueOf(Integer.parseInt(trip[13]) * 60));

                            double activityXCoord = Double.parseDouble(trip[15]);
                            double activityYCoord = Double.parseDouble(trip[16]);
                            int activityEndTime = Integer.parseInt(trip[11]) * 3600 + Integer.parseInt(trip[12]) * 60;

                            Activity actMATSim = populationFactory.createActivityFromCoord(activityPurpose, new Coord(activityXCoord, activityYCoord));
                            actMATSim.setEndTime(activityEndTime);

                            String legMode = trip[8];

                            Leg legMATSim  = populationFactory.createLeg(legMode);

//                            add first activity and the respective leg
                            plan.addActivity(actMATSim);
                            plan.addLeg(legMATSim);

//                            Then follow the 2nd activity

                            String secondActivityPurpose = trip[4].concat("_").concat(String.valueOf(Integer.parseInt(trip[14]) * 60));
                            double secondActivityXCoord = Double.parseDouble(trip[17]);
                            double secondActivityYCoord = Double.parseDouble(trip[18]);

//                            Second activity end time = first activity depart + first trip travel time + second activity duration

                            int secondActivityEndTime = Integer.parseInt(trip[11]) * 3600 + Integer.parseInt(trip[12]) * 60 + Integer.parseInt(trip[10]) * 60 + Integer.parseInt(trip[14]) * 60;

                            Activity secondActMATSim = populationFactory.createActivityFromCoord(secondActivityPurpose, new Coord(secondActivityXCoord, secondActivityYCoord));
                            secondActMATSim.setEndTime(secondActivityEndTime);

//                            add the second activity
                            plan.addActivity(secondActMATSim);

                        } else {

                            String tripsInArray = agentFullTrip.get(i);
                            String[] trip = tripsInArray.split(",");

                            Leg legMATSim = populationFactory.createLeg(trip[8]);

                            plan.addLeg(legMATSim);

//                            add destination activity of the trip
                            String destinationActivityPurpose = trip[4].concat("_").concat(String.valueOf(Integer.parseInt(trip[14]) * 60));
                            Double destinationActivityXCoord = Double.parseDouble(trip[17]);
                            Double destinationActivityYCoord = Double.parseDouble(trip[18]);

                            int destinationActivityEndTime = Integer.parseInt(trip[11]) * 3600 + Integer.parseInt(trip[12]) * 60 + Integer.parseInt(trip[10]) * 60 + Integer.parseInt(trip[14]) * 60;

                            Activity destinationActMATSim = populationFactory.createActivityFromCoord(destinationActivityPurpose, new Coord(destinationActivityXCoord, destinationActivityYCoord));
                            destinationActMATSim.setEndTime(destinationActivityEndTime);

                            plan.addActivity(destinationActMATSim);

                        }
                    }

                    person.addPlan(plan);
                    population.addPerson(person);

                } else {
//                    agents without any trip during the simulated day

                    Geometry homeZoneGeometry = zoneGeometries.get(homeZone);

                    if (homeZoneGeometry == null){
                        // Log missing geometry instead of throwing an exception
                        missingGeometryWriter.write(attributeAgentID + "," + homeZone + "\n");
                        continue;
                    }

//                    generate a random point within the selected polygon representing the home address for agent with no activity during the day
                    Point randomPoint;
                    do {
                        double x = homeZoneGeometry.getEnvelopeInternal().getMinX()
                                + Math.random() * homeZoneGeometry.getEnvelopeInternal().getWidth();
                        double y = homeZoneGeometry.getEnvelopeInternal().getMinY()
                                + Math.random() * homeZoneGeometry.getEnvelopeInternal().getHeight();
                        randomPoint = homeZoneGeometry.getFactory()
                                .createPoint(new org.locationtech.jts.geom.Coordinate(x, y));
                    } while (!homeZoneGeometry.contains(randomPoint));

                    Activity noActivityActivity = populationFactory.createActivityFromCoord("Home_90000", new Coord(randomPoint.getX(), randomPoint.getY()));

                    plan.addActivity(noActivityActivity);

                    person.addPlan(plan);
                    population.addPerson(person);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

//        write the population for further process
        PopulationWriter populationWriter = new PopulationWriter(population);
        populationWriter.write("scenarios/MRDH/planOld.xml.gz");

    }
}
