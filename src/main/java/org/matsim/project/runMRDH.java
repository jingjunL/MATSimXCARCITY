package org.matsim.project;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.ControllerConfigGroup;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.config.groups.ScoringConfigGroup;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.scenario.ScenarioUtils;

public class runMRDH {


    public static void main(String[] args) {

        Config config;
        if ( args==null || args.length==0 || args[0]==null ){
            config = ConfigUtils.loadConfig( "scenarios/BrusselsScenario_10Perc/config.xml" );
        } else {
            config = ConfigUtils.loadConfig( args );
        }
        config.controller().setOverwriteFileSetting( OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists );

        config.qsim().setTrafficDynamics( QSimConfigGroup.TrafficDynamics.kinematicWaves );
        config.qsim().setSnapshotStyle( QSimConfigGroup.SnapshotStyle.kinematicWaves );

        config.qsim().setFlowCapFactor(0.1);
        config.qsim().setStorageCapFactor(0.1);

        config.controller().setRoutingAlgorithmType(ControllerConfigGroup.RoutingAlgorithmType.SpeedyALT);

//        config.global().setNumberOfThreads(12);
//        config.qsim().setNumberOfThreads(11);

        for (long i = 600; i <= 90000; i+=600) {
            config.scoring().addActivityParams(new ScoringConfigGroup.ActivityParams("eatout_" + i).setTypicalDuration(i));
            config.scoring().addActivityParams(new ScoringConfigGroup.ActivityParams("escort_" + i).setTypicalDuration(i));
            config.scoring().addActivityParams(new ScoringConfigGroup.ActivityParams("Home_" + i).setTypicalDuration(i));
            config.scoring().addActivityParams(new ScoringConfigGroup.ActivityParams("others_" + i).setTypicalDuration(i));
            config.scoring().addActivityParams(new ScoringConfigGroup.ActivityParams("school_" + i).setTypicalDuration(i));
            config.scoring().addActivityParams(new ScoringConfigGroup.ActivityParams("shopping_" + i).setTypicalDuration(i));
            config.scoring().addActivityParams(new ScoringConfigGroup.ActivityParams("shopping_" + i).setTypicalDuration(i));
            config.scoring().addActivityParams(new ScoringConfigGroup.ActivityParams("social_" + i).setTypicalDuration(i));
            config.scoring().addActivityParams(new ScoringConfigGroup.ActivityParams("social_" + i).setTypicalDuration(i));
            config.scoring().addActivityParams(new ScoringConfigGroup.ActivityParams("univ_" + i).setTypicalDuration(i));
            config.scoring().addActivityParams(new ScoringConfigGroup.ActivityParams("work_" + i).setTypicalDuration(i));
        }

        config.qsim().setPcuThresholdForFlowCapacityEasing(0.5);

        Scenario scenario = ScenarioUtils.loadScenario(config);


    }

}
