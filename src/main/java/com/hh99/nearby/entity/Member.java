package com.hh99.nearby.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.persistence.*;

@Builder
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Entity
public class Member {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String email;

    @Column(nullable = false)
    private String nickname;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String profileImg;

    //이메일 인증 확인
    @Column(nullable = false)
    private boolean emailCheck;

//    public boolean validatePassword(PasswordEncoder passwordEncoder, String password) {
//        return passwordEncoder.matches(password, this.password);
//    }

    public void update() {
        this.emailCheck = true;
    }
}
