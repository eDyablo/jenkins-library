FROM mcr.microsoft.com/dotnet/core/sdk:3.1

ARG nuget_conf_file=nuget.conf
ARG build_config=Debug

ENV DOTNET_ROLL_FORWARD=Major

COPY src src

COPY ${nuget_conf_file} /root/.nuget/NuGet/NuGet.Config

RUN dotnet build src/nuget-sample --configuration ${build_config}

RUN rm -f /root/.nuget/NuGet/NuGet.Config
