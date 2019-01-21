# Scoold on Kubernetes

[Scoold](https://scoold.com) is a Q&A platform inspired by Stack Overflow.

## TL;DR;

```console
$ helm install ./scoold
```

## Introduction

This chart bootstraps a [Scoold](https://github.com/Erudika/scoold) deployment on a [Kubernetes](http://kubernetes.io) cluster using the [Helm](https://helm.sh) package manager.

This chart **does not** install Para. We offer a fully managed Para service at [ParaIO.com](https://paraio.com)

## Prerequisites

- Kubernetes 1.10+

## Installing the Chart

To install the chart with the release name `my-release`:

```console
$ helm install --name my-release ./scoold
```

The command deploys Scoold on the Kubernetes cluster in the default configuration. The [configuration](#configuration) section lists the parameters that can be configured during installation.

> **Tip**: List all releases using `helm list`

## Uninstalling the Chart

To uninstall/delete the `my-release` deployment:

```console
$ helm delete my-release
```

The command removes all the Kubernetes components associated with the chart and deletes the release.

## Configuration

The following table lists the configurable parameters of the Scoold chart and their default values.

| Parameter                           | Description                                                   | Default                                                  |
|-------------------------------------|---------------------------------------------------------------|----------------------------------------------------------|
| `image.registry`                    | image registry                                                | `docker.io`                                              |
| `image.repository`                  | Scoold Image name                                             | `erudikaltd/scoold`                                      |
| `image.tag`                         | Scoold Image tag                                              | `{VERSION}`                                              |
| `image.pullPolicy`                  | Image pull policy                                             | `Always` if `imageTag` is `latest`, else `IfNotPresent`  |
| `service.type`                      | Kubernetes Service type                                       | `ClusterIP`                                           |
| `service.port`                      | Service HTTP port                                             | `8000`                                                   |
| `service.loadBalancerIP`            | LoadBalancerIP for the Scoold service                         | ``                                                       |
| `service.annotations`               | Service annotations                                           | ``                                                       |
| `applicationConf`                   | Scoold configuration                                          | `{}`                                                     |
| `javaOpts`                          | `JAVA_OPTS` JVM arguments                                     | `-Xmx512m -Xms512m -Dconfig.file=/scoold/config/application.conf` |
| `extraEnvs`                         | Extra ENV variables                                           | ``                                                       |
| `updateStrategy`                    | Update policy                                                 | `RollingUpdate`                                          |
| `ingress.enabled`                   | Enable ingress controller resource                            | `false`                                                  |
| `ingress.annotations`               | Ingress annotations                                           | `[]`                                                     |
| `ingress.certManager`               | Add annotations for cert-manager                              | `false`                                                  |
| `ingress.hosts[0]`                  | Hostname to your Scoold installation                          | `scoold.local`                                           |
| `ingress.tls[0].secretName`         | TLS Secret Name                                               | `Scoold-tls-secret`                                      |
| `ingress.tls[0].hosts`              | TLS Hosts                                                     | `['scoold.local']`                                       |
| `resources`                         | CPU/Memory resource requests/limits                           | Memory: `512Mi`, CPU: `300m`                             |

For more information please refer to the [Scoold README](https://github.com/Erudika/scoold/blob/master/README.md).

A YAML file that specifies the values for the above parameters can be provided while installing the chart. For example,

```console
$ helm install --name my-release -f values.yaml ./scoold
```

> **Tip**: You can use the default [values.yaml](values.yaml)
