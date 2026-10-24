// Copyright 2020-2026 Mirsario & Contributors.
// Modifications Copyright 2026 T2ws.
// Released under the GNU General Public License 3.0.
// See LICENSE.md for details.

package mirsario.cameraoverhaul.utilities;

/// A damped harmonic oscillator. Unlike exponential smoothing, it can overshoot and ring,
/// which is what real heads, necks and shoulders do after an impact or a sudden stop.
public final class Spring {
	private static final double MAX_SUBSTEP = 1.0 / 240.0;

	public double position;
	public double velocity;
	/// Natural frequency, in Hz.
	public double frequency;
	/// 1.0 is critically damped, below 1.0 overshoots and rings, above 1.0 is sluggish.
	public double dampingRatio;

	public Spring(double frequency, double dampingRatio) {
		this.frequency = frequency;
		this.dampingRatio = dampingRatio;
	}

	/// Instantly adds velocity, as if the spring was hit.
	public void impulse(double amount) {
		velocity += amount;
	}

	public double update(double target, double deltaTime) {
		// Never let a lag spike or an unpause blow the simulation up.
		double remaining = MathUtils.clamp(deltaTime, 0.0, 0.1);
		double omega = 2.0 * Math.PI * frequency;
		double k = omega * omega;
		double c = 2.0 * dampingRatio * omega;

		while (remaining > 0.0) {
			double dt = Math.min(remaining, MAX_SUBSTEP);
			remaining -= dt;

			// Semi-implicit Euler: stable for stiff springs.
			double accel = k * (target - position) - c * velocity;
			velocity += accel * dt;
			position += velocity * dt;
		}

		return position;
	}

	public void reset(double value) {
		position = value;
		velocity = 0.0;
	}
}
