namespace nuget_sample
{
  public class Sample
  {
    private string name;

    public Sample(string name) => this.name = name;

    public string Name { get => name; }

    public override bool Equals(object obj)
    {
      return base.Equals(obj);
    }

    public override int GetHashCode()
    {
      return base.GetHashCode();
    }

    public override string ToString()
    {
      return base.ToString();
    }
  }
}
