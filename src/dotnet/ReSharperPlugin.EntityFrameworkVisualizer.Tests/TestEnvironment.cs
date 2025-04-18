﻿using System.Threading;
using JetBrains.Application.BuildScript.Application.Zones;
using JetBrains.ReSharper.Feature.Services;
using JetBrains.ReSharper.Psi.CSharp;
using JetBrains.ReSharper.TestFramework;
using JetBrains.TestFramework;
using JetBrains.TestFramework.Application.Zones;
using NUnit.Framework;

[assembly: Apartment(ApartmentState.STA)]

namespace ReSharperPlugin.EntityFrameworkVisualizer.Tests
{
    [ZoneDefinition]
    public class EntityFrameworkVisualizerTestEnvironmentZone : ITestsEnvZone, IRequire<PsiFeatureTestZone>, IRequire<IEntityFrameworkVisualizerZone> { }

    [ZoneMarker]
    public class ZoneMarker : IRequire<ICodeEditingZone>, IRequire<ILanguageCSharpZone>, IRequire<EntityFrameworkVisualizerTestEnvironmentZone> { }

    [SetUpFixture]
    public class EntityFrameworkVisualizerTestsAssembly : ExtensionTestEnvironmentAssembly<EntityFrameworkVisualizerTestEnvironmentZone> { }
}
