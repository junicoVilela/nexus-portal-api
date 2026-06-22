package br.com.softon.portal.releaseorchestrator.dto.response;

public record TestarJenkinsResponse(
    boolean sucesso,
    String jenkinsUrl,
    String jenkinsJob,
    String erro,
    UltimoBuild ultimoBuild) {

  public record UltimoBuild(
      int number,
      String result,
      boolean building,
      long timestamp,
      long durationMs,
      String url) {}

  public static TestarJenkinsResponse ok(String url, String job, UltimoBuild build) {
    return new TestarJenkinsResponse(true, url, job, null, build);
  }

  public static TestarJenkinsResponse erro(String url, String job, String mensagem) {
    return new TestarJenkinsResponse(false, url, job, mensagem, null);
  }
}
