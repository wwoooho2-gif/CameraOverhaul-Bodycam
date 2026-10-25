// Copyright 2020-2026 Mirsario & Contributors.
// Modifications Copyright 2026 T2ws.
// Released under the GNU General Public License 3.0.
// See LICENSE.md for details.

package mirsario.cameraoverhaul.configuration;

import mirsario.cameraoverhaul.utilities.*;

public final class ConfigData {
	public static final class General {
		public boolean enabled = true;
		public boolean enableInThirdPerson = true;
		public double contextTransitionSmoothing = 0.1;
		// Turning Roll
		public double turningRollAccumulation = 1.0;
		public double turningRollIntensity = 1.25;
		public double turningRollSmoothing = 1.0;
		// Sway
<<<<<<< HEAD
		public double cameraSwayIntensity = 0.60;
=======
		public double cameraSwayIntensity = 0.90;
>>>>>>> 8f86573 (woah)
		public double cameraSwayFrequency = 0.16;
		public double cameraSwayFadeInDelay = 0.15;
		public double cameraSwayFadeInLength = 5.0;
		public double cameraSwayFadeOutLength = 0.75;
		// ScreenShakes
		public double screenShakesMaxIntensity = 2.5;
		public double screenShakesMaxFrequency = 6.0;
		public double explosionTrauma = 1.00;
		public double thunderTrauma = 0.05;
		public double handSwingTrauma = 0.03;
		// Realism
<<<<<<< HEAD
		public double gaitIntensity = 1.0;
		public double gaitCycleLength = 3.6;
=======
		public double walkShakeIntensity = 0.30;
		public double sprintShakeIntensity = 0.38;
		public double gaitIntensity = 1.0;
		public double gaitCycleLength = 3.6;
		public boolean gaitFollowsModel = true;
>>>>>>> 8f86573 (woah)
		public double landingImpactIntensity = 1.0;
		public double jumpKickIntensity = 1.0;
		public double accelerationLeanIntensity = 1.0;
		public double breathingIntensity = 1.0;
		public double fatigueIntensity = 1.0;
		public double handheldTremorIntensity = 1.0;
<<<<<<< HEAD
		// Bodycam
		public boolean bodycamEnabled = true;
		public double bodycamIntensity = 1.0;
		public double bodycamTorsoLag = 1.0;
		public double bodycamStepJolt = 1.0;
	}
	public static final class Contextual implements Cloneable {
		public double strafingRollFactor = 10.0;
=======
		public double mountShakeIntensity = 1.0;
		public double fallShakeIntensity = 1.0;
		public double heartbeatIntensity = 1.0;
		public double footLeanIntensity = 1.0;
		public double terrainLeanIntensity = 1.0;
		// Bodycam
		public boolean bodycamEnabled = true;
		public double bodycamIntensity = 1.0;
		public double bodycamTorsoLag = 1.3;
		public double bodycamStepJolt = 1.0;
	}
	public static final class Contextual implements Cloneable {
		public double strafingRollFactor = 13.0;
>>>>>>> 8f86573 (woah)
		public double forwardVelocityPitchFactor = 7.0;
		public double verticalVelocityPitchFactor = 2.5;
		public double horizontalVelocitySmoothingFactor = 1.0;
		public double verticalVelocitySmoothingFactor = 1.0;
		// Camera smoothing
		public double cameraSmoothing = 0.0;

		public void lerp(Contextual a, Contextual b, double step) {
			strafingRollFactor = MathUtils.lerp(a.strafingRollFactor, b.strafingRollFactor, step);
			forwardVelocityPitchFactor = MathUtils.lerp(a.forwardVelocityPitchFactor, b.forwardVelocityPitchFactor, step);
			verticalVelocityPitchFactor = MathUtils.lerp(a.verticalVelocityPitchFactor, b.verticalVelocityPitchFactor, step);
			horizontalVelocitySmoothingFactor = MathUtils.lerp(a.horizontalVelocitySmoothingFactor, b.horizontalVelocitySmoothingFactor, step);
			verticalVelocitySmoothingFactor = MathUtils.lerp(a.verticalVelocitySmoothingFactor, b.verticalVelocitySmoothingFactor, step);
			cameraSmoothing = MathUtils.lerp(a.cameraSmoothing, b.cameraSmoothing, step);
		}

		public Contextual clone() {
			try { return (Contextual)super.clone(); }
			catch (CloneNotSupportedException _) { return null; }
		}
	}

<<<<<<< HEAD
	public static final int CONFIG_VERSION = 3;
=======
	public static final int CONFIG_VERSION = 4;
>>>>>>> 8f86573 (woah)

	public int configVersion;
	public General general = new General();
	public Contextual walking = new Contextual();
	public Contextual sprinting = new Contextual();
	public Contextual swimming = new Contextual();
	public Contextual flying = new Contextual();
	public Contextual mounts = new Contextual();
	public Contextual vehicles = new Contextual();

	public ConfigData() {
		// Sprinting
<<<<<<< HEAD
		sprinting.strafingRollFactor = 12.5;
		sprinting.forwardVelocityPitchFactor = 9.5;
=======
		sprinting.strafingRollFactor = 16.0;
		sprinting.forwardVelocityPitchFactor = 12.0;
>>>>>>> 8f86573 (woah)
		// Flying
		flying.strafingRollFactor *= -1.0;
		// Swimming
		swimming.strafingRollFactor *= -3.0;
		swimming.forwardVelocityPitchFactor *= 3.0;
		swimming.verticalVelocityPitchFactor *= 3.0;
		// Mounts
		mounts.strafingRollFactor *= 2.0;
		mounts.forwardVelocityPitchFactor *= 0.5;
		// Vehicles
		vehicles.strafingRollFactor *= 0.5;
		vehicles.forwardVelocityPitchFactor *= 0.5;
		vehicles.verticalVelocityPitchFactor *= 2.0;
		// Mouse smoothing
		sprinting.cameraSmoothing = 0.6;
		swimming.cameraSmoothing = 1.5;
		flying.cameraSmoothing = 1.0;
		mounts.cameraSmoothing = 1.0;
		vehicles.cameraSmoothing = 0.0;
	}
}
