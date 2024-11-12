package org.team100.lib.motion.servo;

import java.util.OptionalDouble;

import org.team100.lib.dashboard.Glassy;
import org.team100.lib.profile.Profile100;
import org.team100.lib.state.Control100;

/**
 * Angular position control, e.g. for swerve steering axes or arm axes.
 */
public interface AngularPositionServo extends Glassy {
    /**
     * It is essential to call this after a period of disuse, to prevent transients.
     * 
     * To prevent oscillation, the previous setpoint is used to compute the profile,
     * but there needs to be an initial setpoint.
     */
    void reset();

    void setProfile(Profile100 profile);

    /**
     * Resets the encoder position, is very slow, so 
     * only do this on startup
     * 
     * @param positionRad The position of the encoder
     */
    public void setEncoderPosition(double positionRad);

    void setTorqueLimit(double torqueNm);

    /**
     * The angle measure here *does not* wind up, so 0 and 2pi are the same.
     * 
     * The measurements here are output measurements, e.g. shaft radians, not motor
     * radians.
     * 
     * @param goalRad           radians
     * @param feedForwardTorque used for gravity compensation
     */
    void setPosition(double goalRad, double feedForwardTorqueNm);

    /**
     * The angle measure here *does not* wind up, so 0 and 2pi are the same.
     * 
     * The measurements here are output measurements, e.g. shaft radians, not motor
     * radians.
     * 
     * @param goalRad           radians
     * @param goalVelocityRad_S rad/s
     * @param feedForwardTorque used for gravity compensation
     */
    void setPositionWithVelocity(double goalRad, double goalVelocityRad_S, double feedForwardTorqueNm);

    /**
     * @return Current position measurement, radians.
     */
    OptionalDouble getPosition();

    OptionalDouble getVelocity();

    boolean atSetpoint();

    boolean atGoal();

    double getGoal();

    void stop();

    void close();

    Control100 getSetpoint();

    /** for logging */
    void periodic();

}
