using System;
using Xunit;
using FluentAssertions;
using Moq;
using Microsoft.Extensions.Configuration;

namespace api.Controllers
{
    public class ValuesControllerTest
    {
        [Fact]
        public void Get_returns_default_text_value_from_config()
        {
            // Arrange
            var valueSection = Mock.Of<IConfigurationSection>();
            var config = Mock.Of<IConfiguration>();
            Mock.Get(config).Setup(c => c.GetSection("defaultTextValue"))
                .Returns(valueSection);
            Mock.Get(valueSection).Setup(s => s.Value).Returns("value");
            var controller = new ValuesController(config);
            // Act
            var result = controller.Get(0);
            // Assert
            result.Value.Should().Be("value");
        }
    }
}
