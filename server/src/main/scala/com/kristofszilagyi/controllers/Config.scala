package com.kristofszilagyi.controllers
import com.kristofszilagyi.shared.{JobName, JobUrl}
import com.netaporter.uri.Uri
import net.jcazevedo.moultingyaml._
import net.jcazevedo.moultingyaml.DefaultYamlProtocol._

object JobConfig {
  def wrappedYamlString[T](fromString: String => T)(convertToString: T => String) = new YamlFormat[T] {
    def read(yaml: YamlValue): T = yaml match {
      case YamlString(s) => fromString(s)
      case other => deserializationError(s"Should be a string got: $other")
    }

    def write(t: T): YamlValue = YamlString(convertToString(t))
  }

  implicit val nameFormat: YamlFormat[JobName] = wrappedYamlString(JobName.apply)(_.s)
  implicit val urlFormat: YamlFormat[JobUrl] = wrappedYamlString(s => JobUrl(Uri.parse(s)))(_.u.toString)
  implicit val format: YamlFormat[JobConfig] = yamlFormat2(JobConfig.apply)
}

final case class JobConfig(name: JobName, url: JobUrl)

object JenkinsConfig {
  implicit val format: YamlFormat[JenkinsConfig] = yamlFormat1(JenkinsConfig.apply)
}

final case class JenkinsConfig(jobs: Seq[JobConfig])

object GitConfig {
  implicit val format: YamlFormat[GitConfig] = yamlFormat1(GitConfig.apply)
}

final case class GitConfig(jobs: Seq[JobConfig])

object Config {
  implicit val format: YamlFormat[Config] = yamlFormat2(Config.apply)
}
final case class Config(jenkins: JenkinsConfig, git: GitConfig)