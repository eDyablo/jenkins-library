FROM mcr.microsoft.com/dotnet/core/aspnet:2.2

ARG build_path=build

WORKDIR /app

COPY ${build_path}/app .

EXPOSE 80

ENTRYPOINT [ "dotnet", "main/main.dll" ]
