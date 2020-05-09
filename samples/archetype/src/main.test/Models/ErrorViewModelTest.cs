using System;
using Xunit;
using FluentAssertions;

namespace main.Models
{
    public class ErrorViewModelTest
    {
        [Fact]
        public void ShowRequestId_is_false_when_RequestId_is_null()
        {
            var model = new ErrorViewModel();
            model.RequestId = null;
            model.ShowRequestId.Should().Be(false);
        }
 
        [Fact]
        public void ShowRequestId_is_false_when_RequestId_is_empty()
        {
            var model = new ErrorViewModel();
            model.RequestId = "";
            model.ShowRequestId.Should().Be(false);
        }
    }
}
