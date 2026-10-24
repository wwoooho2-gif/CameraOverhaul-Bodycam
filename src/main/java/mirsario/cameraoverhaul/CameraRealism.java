// Copyright 2020-2026 Mirsario & Contributors.
// Modifications Copyright 2026 T2ws.
// Released under the GNU General Public License 3.0.
// See LICENSE.md for details.

package mirsario.cameraoverhaul;

import mirsario.cameraoverhaul.configuration.*;
import mirsario.cameraoverhaul.utilities.*;
import org.joml.SimplexNoise;
import org.joml.Vector2d;
import org.joml.Vector3d;

/// Physically-inspired camera motion: a gait cycle tied to travel distance, landing and jump impacts,
/// neck inertia, breathing, fatigue, and muscle tremor. Everything is rotation-only and additive.
///
/// With bodycam mode on, the camera behaves as if strapped to the chest instead of sitting on a stabilised head:
/// every footstep jolts it, it lags behind where you look because the torso trails the head, and it has no tremor.
public final class CameraRealism {
	// Minecraft's resting vertical velocity, in blocks/tick (gravity is applied before collision resolution).
	private static final double RESTING_VERTICAL_VELOCITY = -0.0784;
	private static final double GROUNDED_VERTICAL_TOLERANCE = 0.07;
	private static final double WALK_SPEED = 4.317; // Blocks per second.
	private static final double TICK_TIME = 0.05;

	// Springs. Frequencies are in Hz, damping below 1.0 means they ring after being hit.
	private final Spring landingPitch = new Spring(2.4, 0.30);
	private final Spring landingRoll = new Spring(2.0, 0.35);
	private final Spring leanPitch = new Spring(1.8, 0.50);
	private final Spring leanRoll = new Spring(1.6, 0.50);
	// Bodycam
	private final Spring stepPitch = new Spring(5.0, 0.25);
	private final Spring stepRoll = new Spring(4.0, 0.30);
	private final Spring stepYaw = new Spring(3.5, 0.35);
	private final Spring torsoYaw = new Spring(2.6, 0.85);
	private final Spring torsoPitch = new Spring(2.2, 0.85);

	// Gait
	private double gaitCycles; // Continuous, in full cycles (left step + right step).
	private double gaitFade;
	private double groundedFactor = 1.0;
	private long lastStepIndex;
	private double stepSide = 1.0;

	// Torso follow
	private boolean torsoInitialized;
	private double prevYaw, prevPitch;
	private double continuousYaw, continuousPitch;

	// Air state
	private boolean wasGrounded = true;
	private double fallPeak;
	private double landingSide = 1.0;

	// Tick-rate acceleration estimation
	private final Vector3d prevTickVelocity = new Vector3d();
	private final Vector3d acceleration = new Vector3d();
	private double lastVelocityChangeTime;
	private boolean initialized;

	// Fatigue / breathing
	private double fatigue;
	private double breathPhase;

	/// Scales a value towards a bodycam-style gain. At 0 bodycam strength it stays 1, at full strength it becomes gain.
	private static double mix(double strength, double gain) {
		return 1.0 + (gain - 1.0) * strength;
	}

	public void update(CameraContext context, ConfigData.General cfg, Transform output, double deltaTime) {
		if (context.perspective != CameraContext.Perspective.FIRST_PERSON) {
			torsoInitialized = false;
			return;
		}

		double time = TimeSystem.getTime();
		double dt = MathUtils.clamp(deltaTime, 0.0, 0.1);
		var velocity = context.velocity;
		double bodycam = cfg.bodycamEnabled ? MathUtils.clamp(cfg.bodycamIntensity, 0.0, 2.0) : 0.0;

		if (!initialized) {
			prevTickVelocity.set(velocity);
			lastVelocityChangeTime = time;
			initialized = true;
		}

		boolean onFoot = !context.isRiding && !context.isSwimming && !context.isFlying;
		double horizontalSpeed = Math.sqrt(velocity.x * velocity.x + velocity.z * velocity.z) / TICK_TIME;
		boolean groundedNow = onFoot && Math.abs(velocity.y - RESTING_VERTICAL_VELOCITY) < GROUNDED_VERTICAL_TOLERANCE;

		double rotX = 0.0, rotY = 0.0, rotZ = 0.0;

		updateFatigue(context, dt, horizontalSpeed, onFoot);
		updateAcceleration(velocity, time);
		updateAirState(velocity, groundedNow, onFoot, cfg, bodycam);

		// Smoothly blend the gait in and out so that stepping off a ledge or stopping isn't a hard cut.
		double gaitTarget = groundedNow && horizontalSpeed > 0.4 ? 1.0 : 0.0;
		gaitFade = MathUtils.damp(gaitFade, gaitTarget, 0.12, dt);
		groundedFactor = MathUtils.damp(groundedFactor, groundedNow ? 1.0 : 0.0, 0.08, dt);

		// Gait: advance by distance travelled, so the steps always match the speed of the world moving by.
		if (groundedNow && cfg.gaitCycleLength > 0.01) {
			gaitCycles += (horizontalSpeed / cfg.gaitCycleLength) * dt;
		}
		double gaitPhase = gaitCycles - Math.floor(gaitCycles);

		double tau = Math.PI * 2.0;
		double speedFactor = MathUtils.clamp(horizontalSpeed / WALK_SPEED, 0.0, 1.6);
		double sprintFactor = context.isSprinting ? 1.7 : 1.0;
		double fatigueFactor = 1.0 + 0.30 * fatigue * cfg.fatigueIntensity;
		double gaitAmplitude = gaitFade * speedFactor * sprintFactor * fatigueFactor * cfg.gaitIntensity;

		// Two vertical impacts per cycle (one per foot), one lateral sway per cycle. Pelvis rotation adds a touch of yaw.
		double pitchBob = Math.sin(gaitPhase * tau * 2.0) + 0.18 * Math.sin(gaitPhase * tau);
		rotX += pitchBob * 0.42 * gaitAmplitude * mix(bodycam, 2.2);
		rotZ += Math.sin(gaitPhase * tau) * 0.55 * gaitAmplitude * mix(bodycam, 1.8);
		rotY += Math.sin(gaitPhase * tau + Math.PI * 0.5) * 0.28 * gaitAmplitude * mix(bodycam, 1.5);

		// Bodycam: each footfall slams the chest, and the camera rings out afterwards.
		long stepIndex = (long)Math.floor(gaitCycles * 2.0);
		if (stepIndex != lastStepIndex) {
			lastStepIndex = stepIndex;

			if (bodycam > 0.0 && groundedNow && gaitFade > 0.3) {
				double jolt = speedFactor * sprintFactor * fatigueFactor * cfg.bodycamStepJolt * bodycam;
				stepPitch.impulse(1.3 * Math.PI * 2.0 * stepPitch.frequency * jolt);
				stepRoll.impulse(stepSide * 0.9 * Math.PI * 2.0 * stepRoll.frequency * jolt);
				stepYaw.impulse(-stepSide * 0.6 * Math.PI * 2.0 * stepYaw.frequency * jolt);
			}

			stepSide = -stepSide;
		}
		rotX += stepPitch.update(0.0, dt);
		rotZ += stepRoll.update(0.0, dt);
		rotY += stepYaw.update(0.0, dt);

		// Landing, jumping.
		double landingFade = onFoot ? 1.0 : 0.0;
		landingPitch.update(0.0, dt);
		landingRoll.update(0.0, dt);
		rotX += landingPitch.position * landingFade;
		rotZ += landingRoll.position * landingFade;

		// Neck inertia: the head lags behind body acceleration, then overshoots and settles.
		// On a bodycam, the chest takes the whole shove with no neck to absorb it.
		double leanScale = cfg.accelerationLeanIntensity * mix(bodycam, 1.6);
		var accelRel = VectorUtils.rotate(new Vector2d(acceleration.x, acceleration.z), 360d - context.transform.eulerRot.y);
		double leanPitchTarget = MathUtils.clamp(-accelRel.y * 33.0, -4.0, 4.0) * leanScale;
		double leanRollTarget = MathUtils.clamp(accelRel.x * 40.0, -4.0, 4.0) * leanScale;
		rotX += leanPitch.update(leanPitchTarget, dt);
		rotZ += leanRoll.update(leanRollTarget, dt);

		// Bodycam: the torso trails behind where you look, so the camera pans late and settles on target.
		if (bodycam > 0.0 && cfg.bodycamTorsoLag > 0.0) {
			double yawNow = context.transform.eulerRot.y;
			double pitchNow = context.transform.eulerRot.x;

			if (!torsoInitialized) {
				prevYaw = yawNow;
				prevPitch = pitchNow;
				continuousYaw = yawNow;
				continuousPitch = pitchNow;
				torsoYaw.reset(continuousYaw);
				torsoPitch.reset(continuousPitch);
				torsoInitialized = true;
			}

			continuousYaw += MathUtils.unwrapStep(yawNow - prevYaw);
			continuousPitch += MathUtils.unwrapStep(pitchNow - prevPitch);
			prevYaw = yawNow;
			prevPitch = pitchNow;

			double lag = cfg.bodycamTorsoLag * Math.min(bodycam, 1.0);
			// Keep the lag bounded so that what you aim at is never far from the middle of the screen.
			double yawOffset = MathUtils.clamp(torsoYaw.update(continuousYaw, dt) - continuousYaw, -25.0, 25.0);
			double pitchOffset = MathUtils.clamp(torsoPitch.update(continuousPitch, dt) - continuousPitch, -12.0, 12.0);
			rotY += yawOffset * lag;
			rotX += pitchOffset * lag * 0.6;
		} else {
			torsoInitialized = false;
		}

		// Breathing: slow when calm, fast and deep when tired. A chest-mounted camera rides it directly.
		double breathRate = 0.24 + 0.70 * fatigue;
		breathPhase += breathRate * dt;
		breathPhase -= Math.floor(breathPhase);
		double breathAmplitude = (0.07 + 0.38 * fatigue) * (horizontalSpeed > 0.4 ? 0.6 : 1.0) * mix(bodycam, 2.2);
		// Inhale is a bit quicker than exhale.
		double breath = Math.sin(breathPhase * tau + 0.35 * Math.sin(breathPhase * tau));
		rotX += breath * breathAmplitude * cfg.breathingIntensity;

		// Muscle tremor: tiny, fast, never quite still. Gets worse when tired. A strapped-on camera has none of it.
		double tremorAmplitude = (0.025 + 0.07 * fatigue) * cfg.handheldTremorIntensity * mix(bodycam, 0.4);
		float tremorTime = (float)(time * 8.5);
		rotX += SimplexNoise.noise(tremorTime, 11f) * tremorAmplitude;
		rotY += SimplexNoise.noise(tremorTime, 23f) * tremorAmplitude;
		rotZ += SimplexNoise.noise(tremorTime, 37f) * tremorAmplitude * 0.5;

		output.eulerRot.x += rotX;
		output.eulerRot.y += rotY;
		output.eulerRot.z += rotZ;
	}

	private void updateFatigue(CameraContext context, double dt, double horizontalSpeed, boolean onFoot) {
		boolean exerting = onFoot && context.isSprinting && horizontalSpeed > 1.0;
		if (exerting) fatigue += dt / 9.0; // ~9s of sprinting to be properly winded.
		else fatigue -= dt / 14.0; // ~14s to recover.
		fatigue = MathUtils.clamp01(fatigue);
	}

	/// Velocity only changes on game ticks, so estimate acceleration per tick and hold it until the next one.
	private void updateAcceleration(Vector3d velocity, double time) {
		if (!velocity.equals(prevTickVelocity)) {
			acceleration.set(velocity.x - prevTickVelocity.x, 0.0, velocity.z - prevTickVelocity.z);
			prevTickVelocity.set(velocity);
			lastVelocityChangeTime = time;
		} else if (time - lastVelocityChangeTime > TICK_TIME * 1.5) {
			acceleration.set(0.0);
		}
	}

	private void updateAirState(Vector3d velocity, boolean groundedNow, boolean onFoot, ConfigData.General cfg, double bodycam) {
		if (!onFoot) {
			wasGrounded = false;
			fallPeak = 0.0;
			return;
		}

		double omegaPitch = Math.PI * 2.0 * landingPitch.frequency;

		if (wasGrounded && !groundedNow) {
			// Takeoff. A jump pushes up from the legs, so the camera kicks back a little.
			if (velocity.y > 0.25) {
				landingPitch.impulse(-1.3 * omegaPitch * cfg.jumpKickIntensity * mix(bodycam, 1.3));
			}
			fallPeak = 0.0;
		}

		if (!groundedNow) {
			fallPeak = Math.max(fallPeak, -velocity.y);
		} else if (!wasGrounded) {
			// Touchdown. The camera dips by an amount that scales with how fast we were falling, then rings out.
			double impact = MathUtils.clamp((fallPeak - 0.25) / 1.0, 0.0, 1.5);
			if (impact > 0.0) {
				double scale = impact * cfg.landingImpactIntensity * mix(bodycam, 1.5);
				landingPitch.impulse(6.0 * omegaPitch * scale);
				landingRoll.impulse(landingSide * 1.8 * Math.PI * 2.0 * landingRoll.frequency * scale);
				landingSide = -landingSide;
			}
			fallPeak = 0.0;
		}

		wasGrounded = groundedNow;
	}
}
