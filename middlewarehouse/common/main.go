package common

import (
	"errors"
	"fmt"
	"os"
	"regexp"

	env "github.com/jpfuentes2/go-env"
)

// Env Returns the value exported to GOENV in the system. Defaults to 'development'.
func Env() string {
	env := os.Getenv("GOENV")

	if len(env) == 0 {
		env = "development"
	}

	return env
}

// IsEnv helper function for cleaner boolean conditions
//if isEnv("production) { ... } else { ... } //etc
func IsEnv(name string) bool {
	return (Env() == name)
}

func IsProduction() bool {
	return IsEnv("production")
}

func IsTest() bool {
	return IsEnv("test")
}

func IsDevelopment() bool {
	return IsEnv("development")
}

func AppDir() string {
	d, _ := os.Getwd()
	re := regexp.MustCompile("middlewarehouse")
	match := re.FindStringIndex(d)

	if match == nil {
		panic(fmt.Sprintf("Could not locate base path of project! [%s]", d))
	}

	basePath := d[0:match[1]]
	return basePath
}

func MustLoadEnv() {
	basePath := AppDir()
	envFile := fmt.Sprintf("%s/.env.%s", basePath, Env())
	if err := fileExists(envFile); err != nil {
		if Env() == "development" {
			envFile = fmt.Sprintf("%s/.env", basePath)
			if err = fileExists(envFile); err != nil {
				panic(err.Error())
			}
		} else {
			panic(err.Error())
		}
	}

	env.ReadEnv(envFile)
}

func fileExists(path string) error {
	_, err := os.Stat(path)

	if err != nil {
		msg := fmt.Sprintf("Could not locate [%s]", path)
		if !os.IsNotExist(err) {
			msg = fmt.Sprintf("%s - %s", msg, err.Error())
		}
		return errors.New(msg)
	}

	return nil
}
