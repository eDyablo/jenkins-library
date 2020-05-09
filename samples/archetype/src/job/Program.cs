using System;

namespace job
{
    class Program
    {
        static void Main(string[] args)
        {
            var settings = new lib.Settings();
            Console.WriteLine($"Hello World! I'm { settings.AppName }.");
        }
    }
}
