{
  description = "Carykh's Super Sudoku - Android app build environment";

  inputs = {
    nixpkgs.url = "github:NixOS/nixpkgs/nixos-unstable";
  };

  outputs = { self, nixpkgs }:
    let
      system = "x86_64-linux";
      pkgs = import nixpkgs {
        inherit system;
        config = {
          allowUnfree = true;
          android_sdk.accept_license = true;
        };
      };
      androidSdk = (pkgs.androidenv.composeAndroidPackages {
        platformVersions = [ "36" ];
        buildToolsVersions = [ "36.0.0" ];
        includeEmulator = false;
        includeSystemImages = false;
        includeNDK = false;
      }).androidsdk;
    in
    {
      devShells.${system}.default = pkgs.mkShell {
        name = "super-sudoku";
        buildInputs = with pkgs; [
          jdk17
          gradle
          androidSdk
          python3
        ];
        shellHook = ''
          export ANDROID_HOME="${androidSdk}/libexec/android-sdk"
          export ANDROID_SDK_ROOT="$ANDROID_HOME"
          export JAVA_HOME="${pkgs.jdk17}"
          export PATH="$ANDROID_HOME/platform-tools:$ANDROID_HOME/cmdline-tools/latest/bin:$PATH"
          # Keep Gradle user home inside the project to avoid polluting $HOME
          # export GRADLE_USER_HOME="$PWD/.gradle-home"
          echo "Android SDK: $ANDROID_HOME"
          java -version 2>&1 | head -1
        '';
      };
    };
}
