package de.maggu2810.karaf.bpg;

import java.util.List;
import org.apache.karaf.bundle.core.BundleInfo;
import org.apache.karaf.bundle.core.BundleService;
import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

@Command(scope = "bpg", name = "test", description = "bundle pom generator test")
@Service
public class Test implements Action {

  @Option(name = "--context", aliases = {"-c"}, description = "Use the given bundle context")
  String context = "0";

  @Option(name = "-t", valueToShowInHelp = "", description = "Specifies the bundle threshold; bundles with a start-level less than this value will not get printed out.", required = false, multiValued = false)
  int bundleLevelThreshold = -1;

  @Argument(index = 0, name = "ids", description = "The list of bundle (identified by IDs or name or name/version) separated by whitespaces", required = false, multiValued = true)
  List<String> ids;

  @Reference
  BundleContext bundleContext;

  @Reference
  BundleService bundleService;

  public void setBundleService(BundleService bundleService) {
    this.bundleService = bundleService;
  }

  public void setBundleContext(BundleContext bundleContext) {
    this.bundleContext = bundleContext;
  }

  @Override
  public Object execute() throws Exception {
    List<Bundle> bundles = bundleService.selectBundles(context, ids, true);

    System.out.println("<dependencies>");

    for (Bundle bundle : bundles) {
      System.out.println(String.format("<!-- %s-%s -->", bundle.getSymbolicName(), bundle.getVersion().toString()));

      BundleInfo info = this.bundleService.getInfo(bundle);

      final String updateLocation = info.getUpdateLocation();

      if (updateLocation.startsWith("mvn:")) {
        final String gav = updateLocation.replaceFirst("mvn:", "");
        final String[] gavSplit = gav.split("/");
        if (gavSplit.length < 3) {
          continue;
        }

        final String groupId = gavSplit[0];
        final String artifactId = gavSplit[1];
        final String version = gavSplit[2];

        System.out.print("<dependency>");
        System.out.print(String.format("<groupId>%s</groupId>", groupId));
        System.out.print(String.format("<artifactId>%s</artifactId>", artifactId));
        System.out.print(String.format("<version>%s</version>", version));

        switch (gavSplit.length) {
          case 3:
            break;
          case 5:
            final String type = gavSplit[3];
            final String classifier = gavSplit[4];
            System.out.print(String.format("<type>%s</type>", type));
            System.out.print(String.format("<classifier>%s</classifier>", classifier));
            break;
          default:
            System.out.println(String.format("<!-- cannot handle a length of: %d -->", gavSplit.length));
            break;
        }

        System.out.print(String.format("<scope>provided</scope>"));
        System.out.print("</dependency>");
        System.out.println();
      } else {
        System.out.println(String.format("<!-- Cannot handle %s -->", updateLocation));
      }
    }

    System.out.println("</dependencies>");

    return null;
  }
}
