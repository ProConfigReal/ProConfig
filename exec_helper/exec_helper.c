#include <jni.h>
#include <unistd.h>
#include <fcntl.h>
#include <stdlib.h>
#include <string.h>
#include <errno.h>
#include <sys/types.h>

static char *jstring_to_cstr(JNIEnv *env, jstring js) {
    if (js == NULL) return NULL;
    const char *tmp = (*env)->GetStringUTFChars(env, js, NULL);
    if (tmp == NULL) return NULL;
    char *copy = strdup(tmp);
    (*env)->ReleaseStringUTFChars(env, js, tmp);
    return copy;
}

JNIEXPORT jint JNICALL
Java_com_proconfig_app_service_ProcessRunner_nativeStart(
        JNIEnv *env, jclass clazz,
        jstring jpath, jobjectArray jargs, jobjectArray jenv, jintArray jinheritFds) {

    const char *exe = (*env)->GetStringUTFChars(env, jpath, NULL);
    if (exe == NULL) return -1;

    jsize argc = (*env)->GetArrayLength(env, jargs);
    jsize envc = (*env)->GetArrayLength(env, jenv);

    char **argv = (char **) malloc(sizeof(char *) * (argc + 2));
    argv[0] = strdup(exe);
    for (jsize i = 0; i < argc; i++) {
        jstring s = (jstring) (*env)->GetObjectArrayElement(env, jargs, i);
        argv[i + 1] = jstring_to_cstr(env, s);
        (*env)->DeleteLocalRef(env, s);
    }
    argv[argc + 1] = NULL;

    char **envp = (char **) malloc(sizeof(char *) * (envc + 1));
    for (jsize i = 0; i < envc; i++) {
        jstring s = (jstring) (*env)->GetObjectArrayElement(env, jenv, i);
        envp[i] = jstring_to_cstr(env, s);
        (*env)->DeleteLocalRef(env, s);
    }
    envp[envc] = NULL;

    jint nfd = 0;
    jint *fds = NULL;
    if (jinheritFds != NULL) {
        nfd = (*env)->GetArrayLength(env, jinheritFds);
        if (nfd > 0) {
            fds = (*env)->GetIntArrayElements(env, jinheritFds, NULL);
        }
    }

    pid_t pid = fork();

    if (pid == 0) {
        /* child */
        if (fds != NULL) {
            for (jint i = 0; i < nfd; i++) {
                fcntl((int) fds[i], F_SETFD, 0); /* clear FD_CLOEXEC */
            }
        }
        int devnull = open("/dev/null", O_RDWR);
        if (devnull >= 0) {
            dup2(devnull, STDIN_FILENO);
            dup2(devnull, STDOUT_FILENO);
            dup2(devnull, STDERR_FILENO);
            if (devnull > STDERR_FILENO) close(devnull);
        }
        setsid();
        execve(exe, argv, envp);
        _exit(127);
    }

    /* parent */
    (*env)->ReleaseStringUTFChars(env, jpath, exe);
    if (fds != NULL) {
        (*env)->ReleaseIntArrayElements(env, jinheritFds, fds, JNI_ABORT);
    }
    for (jsize i = 0; i <= argc; i++) free(argv[i]);
    free(argv);
    for (jsize i = 0; i < envc; i++) free(envp[i]);
    free(envp);

    if (pid < 0) return -1;
    return (jint) pid;
}
