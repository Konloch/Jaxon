package gui.components;

import gui.Widget;
import kernel.Kernel;

public class FlowFieldView extends Widget
{
	
	protected int _bg;
	private FlowFieldControls _controls;
	private PerlinNoise noise;
	private Random random;
	
	private Particle[] particles;
	private int frameCount = 0;
	
	public FlowFieldView(int x, int y, int width, int height, FlowFieldControls controls)
	{
		super("flowfieldview", x, y, width, height);
		_controls = controls;
		noise = new PerlinNoise();
		random = new Random(1);
		_bg = 0;
		
		BuildParticles(_controls.particleAmount);
	}
	
	private void BuildParticles(int amount)
	{
		particles = new Particle[amount];
		for (int i = 0; i < _controls.particleAmount; i++)
		{
			particles[i] = new Particle(new Vec2f(random.range(0, width), random.range(0, height)), new Vec2f(random.nextFloat(), random.nextFloat()), random.range(3, 5), Kernel.Display.Rgb(255, 255, 255));
		}
	}
	
	public void update()
	{
		if (_controls.particleAmount != particles.length)
		{
			BuildParticles(_controls.particleAmount);
		}
		for (Particle p : particles)
		{
			p.run();
		}
		frameCount++;
	}
	
	@Override
	public void draw()
	{
		renderTarget.Darken(20);
		
		for (Particle p : particles)
		{
			renderTarget.SetPixel((int) p.loc.x, (int) p.loc.y, p.color);
		}
	}
	
	private class Particle
	{
		Vec2f loc;
		Vec2f vel;
		int color;
		float speed;
		int lifetime;
		
		public Particle(Vec2f loc, Vec2f vel, float speed, int color)
		{
			this.loc = loc;
			this.vel = vel;
			this.speed = speed;
			this.color = color;
			this.lifetime = random.range(20, 200);
		}
		
		@SJC.Inline
		public void run()
		{
			move();
			checkEdges();
			checkLifeTime();
			
			float magn = this.vel.mag();
			double fac_x = Math.Abs(this.vel.x) / magn;
			double fac_y = Math.Abs(this.vel.y) / magn;
			double avg = (fac_x + fac_y) / 2;
			this.color = Kernel.Display.Rgb((int) (fac_x * 255), (int) (150 * avg), (int) (fac_y * 255));
			
		}
		
		@SJC.Inline
		public void move()
		{
			float angle = noise.noise(this.loc.x / _controls.noiseScale, this.loc.y / _controls.noiseScale, (frameCount * _controls.noiseChangeSpeed) / _controls.noiseScale) * Math.TWO_PI * _controls.noiseStrength;
			this.vel.x = Math.Cos(angle);
			this.vel.y = Math.Sin(angle);
			this.vel.mult(speed);
			this.loc.add(this.vel);
		}
		
		@SJC.Inline
		public void checkEdges()
		{
			if (this.loc.x > width || this.loc.x < 0)
				this.loc.x = random.range(0, width);
			
			if (this.loc.y > height || this.loc.y < 0)
				this.loc.y = random.range(0, height);
		}
		
		@SJC.Inline
		public void checkLifeTime()
		{
			if (lifetime <= 0)
			{
				this.loc.x = random.range(0, width);
				this.loc.y = random.range(0, height);
				this.lifetime = random.range(20, 200);
			}

			lifetime--;
		}
	}
	
	private class Vec2f
	{
		float x;
		float y;
		
		public Vec2f(float x, float y)
		{
			this.x = x;
			this.y = y;
		}
		
		@SJC.Inline
		public void add(Vec2f v)
		{
			this.x += v.x;
			this.y += v.y;
		}
		
		@SJC.Inline
		public void mult(float n)
		{
			this.x *= n;
			this.y *= n;
		}
		
		@SJC.Inline
		public float mag()
		{
			return Math.Sqrt(x * x + y * y);
		}
	}
	
	public class Random
	{
		private long seed;
		private static final long a = 25214903917L;
		private static final long c = 11L;
		private static final long m = (1L << 48);
		
		public Random(long seed)
		{
			this.seed = seed;
		}
		
		@SJC.Inline
		public int nextInt(int bound)
		{
			seed = (a * seed + c) % m;
			return (int) (seed % bound);
		}
		
		@SJC.Inline
		public float nextFloat()
		{
			return (float) nextInt(Integer.MAX) / Integer.MAX;
		}
		
		@SJC.Inline
		public int range(int min, int max)
		{
			return nextInt(max - min) + min;
		}
	}
	
	static class PerlinNoise
	{
		private static final int[] Permutation = {151, 160, 137, 91, 90, 15, 131, 13, 201, 95, 96, 53, 194, 233, 7, 225, 140, 36, 103, 30, 69, 142, 8, 99, 37, 240, 21, 10, 23, 190, 6, 148, 247, 120, 234, 75, 0, 26, 197, 62, 94, 252, 219, 203, 117, 35, 11, 32, 57, 177, 33, 88, 237, 149, 56, 87, 174, 20, 125, 136, 171, 168, 68, 175, 74, 165, 71, 134, 139, 48, 27, 166, 77, 146, 158, 231, 83, 111, 229, 122, 60, 211, 133, 230, 220, 105, 92, 41, 55, 46, 245, 40, 244, 102, 143, 54, 65, 25, 63, 161, 1, 216, 80, 73, 209, 76, 132, 187, 208, 89, 18, 169, 200, 196, 135, 130, 116, 188, 159, 86, 164, 100, 109, 198, 173, 186, 3, 64, 52, 217, 226, 250, 124, 123, 5, 202, 38, 147, 118, 126, 255, 82, 85, 212, 207, 206, 59, 227, 47, 16, 58, 17, 182, 189, 28, 42, 223, 183, 170, 213, 119, 248, 152, 2, 44, 154, 163, 70, 221, 153, 101, 155, 167, 43, 172, 9, 129, 22, 39, 253, 19, 98, 108, 110, 79, 113, 224, 232, 178, 185, 112, 104, 218, 246, 97, 228, 251, 34, 242, 193, 238, 210, 144, 12, 191, 179, 162, 241, 81, 51, 145, 235, 249, 14, 239, 107, 49, 192, 214, 31, 181, 199, 106, 157, 184, 84, 204, 176, 115, 121, 50, 45, 127, 4, 150, 254, 138, 236, 205, 93, 222, 114, 67, 29, 24, 72, 243, 141, 128, 195, 78, 66, 215, 61, 156, 180};
		private int[] p;
		
		public PerlinNoise()
		{
			
			p = new int[512];
			for (int i = 0; i < 256; i++)
			{
				p[256 + i] = p[i] = Permutation[i];
			}
		}
		
		public float noise(float x, float y, float z)
		{
			int X = (int) Math.floor(x) & 255;
			int Y = (int) Math.floor(y) & 255;
			int Z = (int) Math.floor(z) & 255;
			
			x -= Math.floor(x);
			y -= Math.floor(y);
			z -= Math.floor(z);
			
			float u = fade(x);
			float v = fade(y);
			float w = fade(z);
			
			int A = p[X] + Y;
			int AA = p[A] + Z;
			int AB = p[A + 1] + Z;
			int B = p[X + 1] + Y;
			int BA = p[B] + Z;
			int BB = p[B + 1] + Z;
			
			return lerp(w, lerp(v, lerp(u, grad(p[AA], x, y, z), grad(p[BA], x - 1, y, z)), lerp(u, grad(p[AB], x, y - 1, z), grad(p[BB], x - 1, y - 1, z))), lerp(v, lerp(u, grad(p[AA + 1], x, y, z - 1), grad(p[BA + 1], x - 1, y, z - 1)), lerp(u, grad(p[AB + 1], x, y - 1, z - 1), grad(p[BB + 1], x - 1, y - 1, z - 1))));
		}
		
		@SJC.Inline
		private float fade(float t)
		{
			return t * t * t * (t * (t * 6 - 15) + 10);
		}
		
		@SJC.Inline
		private float lerp(float t, float a, float b)
		{
			return a + t * (b - a);
		}
		
		@SJC.Inline
		private float grad(int hash, float x, float y, float z)
		{
			int h = hash & 15;
			float u = h < 8 ? x : y;
			float v = h < 4 ? y : h == 12 || h == 14 ? x : z;
			return ((h & 1) == 0 ? u : -u) + ((h & 2) == 0 ? v : -v);
		}
	}
}
