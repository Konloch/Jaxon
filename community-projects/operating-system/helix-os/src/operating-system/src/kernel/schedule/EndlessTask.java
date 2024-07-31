package kernel.schedule;

import kernel.Kernel;

public class EndlessTask extends Task {
    public EndlessTask() {
        super("endless_task");
    }

    @Override
    public void Run() {
        int r = 200;
        int g = 160;
        int b = 100;
        int d_r = 1;
        int d_g = 1;
        int d_b = 1;
        while (true) {
            r += d_r;
            g += d_g;
            b += d_b;
            if (r >= 255 || r <= 0) {
                d_r = -d_r;
            }
            if (g >= 255 || g <= 0) {
                d_g = -d_g;
            }
            if (b >= 255 || b <= 0) {
                d_b = -d_b;
            }

            int x = Kernel.Display.Rgb(r, g, b);
            Kernel.Display.Rectangle(r, r, b, b, x);
            Kernel.Display.Swap();
        }
    }
}
