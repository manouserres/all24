package org.team100.lib.motion.arm;

import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.math.util.Units;
import edu.wpi.first.wpilibj.smartdashboard.Mechanism2d;
import edu.wpi.first.wpilibj.smartdashboard.MechanismLigament2d;
import edu.wpi.first.wpilibj.smartdashboard.MechanismRoot2d;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import edu.wpi.first.wpilibj.util.Color;
import edu.wpi.first.wpilibj.util.Color8Bit;

/**
 * Visualization for 2-dof arm.
 * 
 * It's in a separate class to avoid clutter.
 */
public class ArmVisualization {

    private final ArmSubsystem m_armSubsystem;

    private final Mechanism2d m_sideView;
    private final MechanismLigament2d m_boomLigament;
    private final MechanismLigament2d m_stickLigament;

    public ArmVisualization(ArmSubsystem armSubsystem) {
        m_armSubsystem = armSubsystem;

        ArmAngles angles = m_armSubsystem.getPosition();

        m_sideView = new Mechanism2d(100, 100);

        MechanismRoot2d m_sideRoot = m_sideView.getRoot("SideRoot", 50, 50);

        m_boomLigament = new MechanismLigament2d("Boom",
                25, Units.radiansToDegrees(angles.th1), 5, new Color8Bit(Color.kWhite));
        m_sideRoot.append(m_boomLigament);

        m_stickLigament = new MechanismLigament2d("Stick",
                25, Units.radiansToDegrees(angles.th2 - angles.th1), 5, new Color8Bit(Color.kLightGreen));
        m_boomLigament.append(m_stickLigament);

        SmartDashboard.putData("SideView", m_sideView);
    }

    public void periodic() {
        ArmAngles angles = m_armSubsystem.getPosition();
        m_boomLigament.setAngle(Units.radiansToDegrees(angles.th1));
        m_stickLigament.setAngle(Units.radiansToDegrees(angles.th2 - angles.th1));
    }

}
