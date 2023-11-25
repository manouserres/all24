package org.team100.lib.motor;

import org.team100.lib.motor.drive.Motor100;

public class MockMotor100<T> implements Motor100<T> {
    public double output = 0;
    public double velocity = 0;

    @Override
    public double get() {
        return 0;
    }

    @Override
    public void setDutyCycle(double output) {
        this.output = output;
    }

    @Override
    public void setVelocity(double velocity, double accel) {
        this.velocity = velocity;
    }

}
