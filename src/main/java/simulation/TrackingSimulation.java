package simulation;

import control.DriveController;
import control.DriveMotionPlanner;
import javafx.application.Application;
import javafx.stage.Stage;
import lib.geometry.Pose2d;
import lib.geometry.Pose2dWithCurvature;
import lib.geometry.Rotation2d;
import lib.trajectory.Trajectory;
import lib.trajectory.timing.CentripetalAccelerationConstraint;
import lib.trajectory.timing.TimedState;
import lib.trajectory.timing.TimingConstraint;
import lib.util.ReflectingCSVWriter;
import odometry.Kinematics;
import odometry.RobotState;
import odometry.RobotStateEstimator;
import paths.TrajectoryGenerator;
import paths.autos.FarScaleAuto;
import paths.autos.NearScaleAuto;
import profiles.LockdownProfile;
import profiles.RobotProfile;
import ui.FieldWindow;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;

public class TrackingSimulation {

    private final double kDt;

    private DriveController mDriveController = new DriveController(new LockdownProfile(), 0.01);


    // in / s
    private final double kMaxLinearVel = 130.0; // 10 ft/s -> 120 in / s
    private final double kMaxLinearAccel = 130.0;
    private final double kMaxCentripetalAccel = /*100.0*/70.0;
    private final double kMaxVoltage = 9.0;

    private final List<TimingConstraint<Pose2dWithCurvature>> kTrajectoryConstraints = Arrays.asList(new CentripetalAccelerationConstraint(kMaxCentripetalAccel));

    private ReflectingCSVWriter<Pose2d> csvPoseWriter;
    private ReflectingCSVWriter<DriveMotionPlanner> csvDrivePlanner;

    private TrajectoryGenerator mTrajectoryGenerator = new TrajectoryGenerator(mDriveController.getDriveMotionPlanner());
    private DriveSimulation mDriveSimulation;

    public TrackingSimulation(double pDt) {
        kDt = pDt;
        csvPoseWriter = new ReflectingCSVWriter<>("tracking.csv", Pose2d.class);
        csvDrivePlanner = new ReflectingCSVWriter<>("trajectory.csv", DriveMotionPlanner.class);
        mDriveSimulation = new DriveSimulation(mDriveController, csvPoseWriter, csvDrivePlanner, kDt);
    }

    public void simulate() {

        double timeDriven = 0.0;

        timeDriven += mDriveSimulation.driveTrajectory(generate(NearScaleAuto.kToScalePath), true);
        timeDriven += mDriveSimulation.driveTrajectory(generate(NearScaleAuto.kAtScale.getRotation(), NearScaleAuto.kTurnFromScaleToFirstCube.getRotation()));
        timeDriven += mDriveSimulation.driveTrajectory(generate(NearScaleAuto.kScaleToFirstCubePath), false);
        timeDriven += mDriveSimulation.driveTrajectory(generate(NearScaleAuto.kScaleToFirstCube.getRotation(), NearScaleAuto.kTurnFromFirstCubeToScale.getRotation()));
        timeDriven += mDriveSimulation.driveTrajectory(generate(NearScaleAuto.kFirstCubeToScalePath), false);

        System.out.println("Time Driven:" + timeDriven);

    }

    public Trajectory<TimedState<Pose2dWithCurvature>> generate(List<Pose2d> waypoints) {
        return generate(false, waypoints);
    }

    public Trajectory<TimedState<Pose2dWithCurvature>> generate(boolean reversed, List<Pose2d> waypoints) {
        return mTrajectoryGenerator.generateTrajectory(reversed, waypoints, kTrajectoryConstraints, kMaxLinearVel, kMaxLinearAccel, kMaxVoltage);
    }

    public Trajectory<TimedState<Rotation2d>> generate(double initialHeading, double finalHeading ) {
        return mTrajectoryGenerator.generateTurnInPlaceTrajectory(initialHeading, finalHeading, kTrajectoryConstraints,0.0, kMaxLinearVel, kMaxLinearAccel, kMaxVoltage);
    }

    public Trajectory<TimedState<Rotation2d>> generate(Rotation2d initialHeading, Rotation2d finalHeading ) {
        return mTrajectoryGenerator.generateTurnInPlaceTrajectory(initialHeading, finalHeading, kTrajectoryConstraints,0.0, kMaxLinearVel, kMaxLinearAccel, kMaxVoltage);
    }

    public DriveSimulation getDriveSimulation() {
        return mDriveSimulation;
    }

    public RobotStateEstimator getRobotStateEstimator() {
        return mDriveController.getRobotStateEstimator();
    }

}
