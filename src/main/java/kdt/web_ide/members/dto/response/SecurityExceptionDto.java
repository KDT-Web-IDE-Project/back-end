package kdt.web_ide.members.dto.response;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class SecurityExceptionDto {

  private int statusCode;
  private String msg;

  public SecurityExceptionDto(int statusCode, String msg) {
    this.statusCode = statusCode;
    this.msg = msg;
  }
}
